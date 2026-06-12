package com.ticketsystem.frontend.service;

import com.ticketsystem.frontend.model.TicketFX;
import com.ticketsystem.frontend.util.ApiClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TicketApiService {
    public List<TicketFX> getAllTickets() throws Exception {
        TicketFX[] arr = ApiClient.get("/tickets", TicketFX[].class);
        return Arrays.asList(arr);
    }

    public List<TicketFX> getAssignedTickets() throws Exception {
        TicketFX[] arr = ApiClient.get("/tickets/assigned", TicketFX[].class);
        return Arrays.asList(arr);
    }

    public TicketFX getTicketDetails(String id) throws Exception {
        return ApiClient.get("/tickets/" + id, TicketFX.class);
    }

    public TicketFX createTicket(Object req) throws Exception {
        return ApiClient.post("/tickets", req, TicketFX.class);
    }

    public TicketFX updateTicket(String id, Object req) throws Exception {
        return ApiClient.put("/tickets/" + id, req, TicketFX.class);
    }

    public List<TicketFX> searchTickets(String query, String status, String priority) throws Exception {
        StringBuilder path = new StringBuilder("/tickets/search?");
        if (query != null && !query.isBlank()) path.append("q=").append(URLEncoder.encode(query, StandardCharsets.UTF_8)).append("&");
        if (status != null && !status.isBlank() && !"Alle".equals(status)) path.append("status=").append(status).append("&");
        if (priority != null && !priority.isBlank() && !"Alle".equals(priority)) path.append("priority=").append(priority).append("&");
        TicketFX[] arr = ApiClient.get(path.toString(), TicketFX[].class);
        return Arrays.asList(arr);
    }

    public TicketFX assignTicket(String id, String agentId) throws Exception {
        return ApiClient.patch("/tickets/" + id + "/assign", Map.of("agentId", agentId), TicketFX.class);
    }

    public TicketFX takeTicket(String id) throws Exception {
        return ApiClient.patch("/tickets/" + id + "/take", Map.of(), TicketFX.class);
    }

    public TicketFX sendFeedback(String id, int rating, String feedback) throws Exception {
        return ApiClient.post("/tickets/" + id + "/feedback", Map.of("rating", rating, "feedback", feedback == null ? "" : feedback), TicketFX.class);
    }

    @SuppressWarnings("unchecked")
    public int escalateOverdueTickets() throws Exception {
        Map<String, Object> result = ApiClient.post("/tickets/escalate-overdue", Map.of(), Map.class);
        Object count = result.get("escalated");
        if (count instanceof Number number) return number.intValue();
        return 0;
    }

    public void deleteTicket(String id) throws Exception {
        ApiClient.delete("/tickets/" + id);
    }

    // Feature 17: Ähnliche Tickets
    public List<TicketFX> findSimilar(String title, String description) throws Exception {
        String path = "/tickets/search/similar?title=" + URLEncoder.encode(title == null ? "" : title, StandardCharsets.UTF_8)
                + "&description=" + URLEncoder.encode(description == null ? "" : description, StandardCharsets.UTF_8);
        TicketFX[] arr = ApiClient.get(path, TicketFX[].class);
        return Arrays.asList(arr);
    }

    // Feature 18: Duplikat-Erkennung
    public List<TicketFX> findDuplicates(String title, String description) throws Exception {
        String path = "/tickets/search/duplicates?title=" + URLEncoder.encode(title == null ? "" : title, StandardCharsets.UTF_8)
                + "&description=" + URLEncoder.encode(description == null ? "" : description, StandardCharsets.UTF_8);
        TicketFX[] arr = ApiClient.get(path, TicketFX[].class);
        return Arrays.asList(arr);
    }

    // Feature 38: Ticket wiedereröffnen
    public TicketFX reopenTicket(String id) throws Exception {
        return ApiClient.patch("/tickets/" + id + "/reopen", Map.of(), TicketFX.class);
    }
}