package com.ticketsystem.frontend.service;

import com.ticketsystem.frontend.util.ApiClient;

import java.util.Map;

public class DemoDataApiService {
    @SuppressWarnings("unchecked")
    public String generateDemoData() throws Exception {
        Map<String, Object> response = ApiClient.post("/demo-data/generate", Map.of(), Map.class);
        Object message = response.get("message");
        return message == null ? "Demo-Daten wurden erstellt." : message.toString();
    }
}
