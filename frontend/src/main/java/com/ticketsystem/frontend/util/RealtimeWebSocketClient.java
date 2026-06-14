package com.ticketsystem.frontend.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ticketsystem.frontend.model.RealtimeEventFX;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Small STOMP 1.2 client based on the JDK WebSocket API.
 * The socket only receives refresh signals; protected ticket data is still loaded through REST.
 */
public final class RealtimeWebSocketClient implements WebSocket.Listener {

    private static final String DEFAULT_URL = "ws://localhost:8080/ws";
    private static final RealtimeWebSocketClient INSTANCE = new RealtimeWebSocketClient();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final Map<String, Consumer<RealtimeEventFX>> viewListeners = new ConcurrentHashMap<>();
    private final ScheduledExecutorService reconnectExecutor;
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final StringBuilder incoming = new StringBuilder();

    private volatile WebSocket webSocket;
    private volatile boolean connected;
    private volatile boolean stopped = true;

    private RealtimeWebSocketClient() {
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "ticket-realtime-reconnect");
            thread.setDaemon(true);
            return thread;
        };
        reconnectExecutor = Executors.newSingleThreadScheduledExecutor(factory);
    }

    public static RealtimeWebSocketClient getInstance() {
        return INSTANCE;
    }

    public void setViewListener(String viewId, Consumer<RealtimeEventFX> listener) {
        if (viewId == null || listener == null) {
            return;
        }
        viewListeners.put(viewId, listener);
        start();
    }

    public void clearViewListeners() {
        viewListeners.clear();
    }

    public synchronized void start() {
        stopped = false;
        if (connected || webSocket != null || !connecting.compareAndSet(false, true)) {
            return;
        }

        httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .buildAsync(resolveWebSocketUri(), this)
                .whenComplete((socket, error) -> {
                    connecting.set(false);
                    if (error != null) {
                        scheduleReconnect();
                    } else {
                        webSocket = socket;
                    }
                });
    }

    public synchronized void disconnect() {
        stopped = true;
        connected = false;
        connecting.set(false);
        viewListeners.clear();
        WebSocket socket = webSocket;
        webSocket = null;
        if (socket != null) {
            try {
                socket.sendText("DISCONNECT\n\n\0", true);
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "Logout");
            } catch (Exception ignored) {
                socket.abort();
            }
        }
    }

    @Override
    public void onOpen(WebSocket socket) {
        webSocket = socket;
        String connectFrame = "CONNECT\n"
                + "accept-version:1.2\n"
                + "host:localhost\n"
                + "heart-beat:0,0\n\n\0";
        socket.sendText(connectFrame, true);
        socket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
        synchronized (incoming) {
            incoming.append(data);
            processCompleteFrames();
        }
        socket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
        connected = false;
        webSocket = null;
        scheduleReconnect();
        return null;
    }

    @Override
    public void onError(WebSocket socket, Throwable error) {
        connected = false;
        webSocket = null;
        scheduleReconnect();
    }

    private void processCompleteFrames() {
        int terminator;
        while ((terminator = incoming.indexOf("\0")) >= 0) {
            String frame = incoming.substring(0, terminator);
            incoming.delete(0, terminator + 1);
            handleFrame(frame);
        }
    }

    private void handleFrame(String rawFrame) {
        String frame = rawFrame.replaceFirst("^[\r\n]+", "");
        if (frame.isBlank()) {
            return;
        }

        int firstLineEnd = frame.indexOf('\n');
        String command = (firstLineEnd < 0 ? frame : frame.substring(0, firstLineEnd)).trim();

        if ("CONNECTED".equals(command)) {
            connected = true;
            WebSocket socket = webSocket;
            if (socket != null) {
                String subscribeFrame = "SUBSCRIBE\n"
                        + "id:realtime-ui\n"
                        + "destination:/topic/realtime\n"
                        + "ack:auto\n\n\0";
                socket.sendText(subscribeFrame, true);
            }
            return;
        }

        if (!"MESSAGE".equals(command)) {
            return;
        }

        int bodyStart = frame.indexOf("\n\n");
        int separatorLength = 2;
        if (bodyStart < 0) {
            bodyStart = frame.indexOf("\r\n\r\n");
            separatorLength = 4;
        }
        if (bodyStart < 0) {
            return;
        }

        String jsonBody = frame.substring(bodyStart + separatorLength).trim();
        try {
            RealtimeEventFX event = objectMapper.readValue(jsonBody, RealtimeEventFX.class);
            Platform.runLater(() -> viewListeners.values().forEach(listener -> {
                try {
                    listener.accept(event);
                } catch (RuntimeException ignored) {
                    // One view must not prevent other listeners from receiving updates.
                }
            }));
        } catch (Exception ignored) {
            // Ignore malformed refresh messages; REST data remains unaffected.
        }
    }

    private void scheduleReconnect() {
        if (stopped || viewListeners.isEmpty()) {
            return;
        }
        reconnectExecutor.schedule(this::start, 3, TimeUnit.SECONDS);
    }

    private URI resolveWebSocketUri() {
        String configured = System.getProperty("ticket.websocket.url");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("TICKET_WEBSOCKET_URL");
        }
        return URI.create(configured == null || configured.isBlank() ? DEFAULT_URL : configured.trim());
    }
}
