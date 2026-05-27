package com.ticketsystem.frontend.service;

import com.ticketsystem.frontend.model.KnowledgeBaseFX;
import com.ticketsystem.frontend.util.ApiClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class KnowledgeBaseApiService {
    public List<KnowledgeBaseFX> search(String query) throws Exception {
        String path = "/knowledge-base";
        if (query != null && !query.isBlank()) {
            path += "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        }
        KnowledgeBaseFX[] arr = ApiClient.get(path, KnowledgeBaseFX[].class);
        return Arrays.asList(arr);
    }

    public KnowledgeBaseFX create(String title, String category, String solution, String keywords, String template) throws Exception {
        return ApiClient.post("/knowledge-base", Map.of(
                "title", title,
                "category", category,
                "solution", solution,
                "keywords", keywords == null ? "" : keywords,
                "answerTemplate", template == null ? solution : template,
                "active", true
        ), KnowledgeBaseFX.class);
    }

    public void delete(String id) throws Exception {
        ApiClient.delete("/knowledge-base/" + id);
    }
}
