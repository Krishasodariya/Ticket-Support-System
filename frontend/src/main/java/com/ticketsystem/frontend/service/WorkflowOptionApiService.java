package com.ticketsystem.frontend.service;

import com.ticketsystem.frontend.model.WorkflowOptionFX;
import com.ticketsystem.frontend.util.ApiClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class WorkflowOptionApiService {
    public List<WorkflowOptionFX> get(String type) throws Exception {
        WorkflowOptionFX[] arr = ApiClient.get("/workflow-options/" + type + "?activeOnly=false", WorkflowOptionFX[].class);
        return Arrays.asList(arr);
    }

    public WorkflowOptionFX create(String type, String name, String label, int sortOrder) throws Exception {
        return ApiClient.post("/workflow-options", Map.of(
                "type", type,
                "name", name,
                "label", label,
                "sortOrder", sortOrder,
                "active", true
        ), WorkflowOptionFX.class);
    }

    public void delete(Long id) throws Exception {
        ApiClient.delete("/workflow-options/" + id);
    }
}
