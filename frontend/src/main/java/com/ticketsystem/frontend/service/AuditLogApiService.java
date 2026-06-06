package com.ticketsystem.frontend.service;

import com.ticketsystem.frontend.model.AuditLogFX;
import com.ticketsystem.frontend.util.ApiClient;

import java.util.Arrays;
import java.util.List;

public class AuditLogApiService {
    public List<AuditLogFX> getAllLogs() throws Exception {
        AuditLogFX[] arr = ApiClient.get("/audit-logs", AuditLogFX[].class);
        return Arrays.asList(arr);
    }

    public List<AuditLogFX> getLogsForTicket(String ticketId) throws Exception {
        AuditLogFX[] arr = ApiClient.get("/audit-logs/ticket/" + ticketId, AuditLogFX[].class);
        return Arrays.asList(arr);
    }
}
