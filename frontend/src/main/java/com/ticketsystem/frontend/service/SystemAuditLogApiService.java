package com.ticketsystem.frontend.service;

import com.ticketsystem.frontend.model.SystemAuditLogFX;
import com.ticketsystem.frontend.util.ApiClient;

import java.util.Arrays;
import java.util.List;

/** Feature 32 – System-Aktivitätsprotokoll */
public class SystemAuditLogApiService {

    public List<SystemAuditLogFX> getAll() throws Exception {
        SystemAuditLogFX[] arr = ApiClient.get("/system-audit-logs", SystemAuditLogFX[].class);
        return Arrays.asList(arr);
    }

    public List<SystemAuditLogFX> getByType(String eventType) throws Exception {
        SystemAuditLogFX[] arr = ApiClient.get("/system-audit-logs?type=" + eventType, SystemAuditLogFX[].class);
        return Arrays.asList(arr);
    }
}

