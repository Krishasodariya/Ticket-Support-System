package com.ticketsystem.frontend.service;

import com.ticketsystem.frontend.model.DashboardStatsFX;
import com.ticketsystem.frontend.util.ApiClient;

public class DashboardApiService {
    public DashboardStatsFX getStats() throws Exception {
        return ApiClient.get("/dashboard/stats", DashboardStatsFX.class);
    }

    public byte[] exportTicketsCsv() throws Exception {
        return ApiClient.downloadBytes("/dashboard/export/tickets.csv");
    }

    public byte[] exportTicketsCsv(String status, String priority, String query) throws Exception {
        return ApiClient.downloadBytes(exportPath("/dashboard/export/tickets.csv", status, priority, query));
    }

    public byte[] exportTicketsPdf() throws Exception {
        return ApiClient.downloadBytes("/dashboard/export/tickets.pdf");
    }

    public byte[] exportTicketsPdf(String status, String priority, String query) throws Exception {
        return ApiClient.downloadBytes(exportPath("/dashboard/export/tickets.pdf", status, priority, query));
    }

    private String exportPath(String base, String status, String priority, String query) {
        StringBuilder path = new StringBuilder(base).append("?");
        if (status != null && !status.isBlank() && !"Alle".equals(status)) path.append("status=").append(status).append("&");
        if (priority != null && !priority.isBlank() && !"Alle".equals(priority)) path.append("priority=").append(priority).append("&");
        if (query != null && !query.isBlank()) path.append("q=").append(java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8));
        return path.toString();
    }
}
