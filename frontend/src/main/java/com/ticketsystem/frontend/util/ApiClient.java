package com.ticketsystem.frontend.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiClient {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static <T> T get(String path, Class<T> responseType) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .GET();
        
        addAuthHeader(builder);
        
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        handleError(response);
        
        return mapper.readValue(response.body(), responseType);
    }

    public static <T> T post(String path, Object body, Class<T> responseType) throws Exception {
        String json = body != null ? mapper.writeValueAsString(body) : "";
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
                
        addAuthHeader(builder);

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        handleError(response);
        
        if (responseType == Void.class || response.body() == null || response.body().isEmpty()) {
            return null;
        }
        return mapper.readValue(response.body(), responseType);
    }

    public static <T> T put(String path, Object body, Class<T> responseType) throws Exception {
        String json = mapper.writeValueAsString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json));
                
        addAuthHeader(builder);

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        handleError(response);

        if (responseType == Void.class || response.body() == null || response.body().isEmpty()) {
            return null;
        }
        return mapper.readValue(response.body(), responseType);
    }
    
    public static <T> T patch(String path, Object body, Class<T> responseType) throws Exception {
        String json = mapper.writeValueAsString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json));
                
        addAuthHeader(builder);

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        handleError(response);

        if (responseType == Void.class || response.body() == null || response.body().isEmpty()) {
            return null;
        }
        return mapper.readValue(response.body(), responseType);
    }


    public static byte[] downloadBytes(String path) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .GET();

        addAuthHeader(builder);

        HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("API Error: " + response.statusCode());
        }
        return response.body();
    }

    public static void delete(String path) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .DELETE();
                
        addAuthHeader(builder);

        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        handleError(response);
    }

    private static void addAuthHeader(HttpRequest.Builder builder) {
        if (SessionManager.isLoggedIn()) {
            builder.header("Authorization", "Bearer " + SessionManager.getToken());
        }
    }

    private static void handleError(HttpResponse<String> response) throws Exception {
        if (response.statusCode() >= 400) {
            String message = "API Error: " + response.statusCode();
            try {
                message = mapper.readTree(response.body()).path("message").asText(message);
            } catch (Exception ignored) {}
            throw new RuntimeException(message);
        }
    }
}
