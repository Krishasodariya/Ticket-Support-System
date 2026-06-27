package com.ticketsystem.service;

import com.ticketsystem.dto.request.KnowledgeBaseRequest;
import com.ticketsystem.dto.response.KnowledgeBaseResponse;
import com.ticketsystem.exception.ResourceNotFoundException;
import com.ticketsystem.model.KnowledgeBaseArticle;
import com.ticketsystem.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {
    private final KnowledgeBaseRepository repository;

    public KnowledgeBaseService(KnowledgeBaseRepository repository) {
        this.repository = repository;
    }

    public List<KnowledgeBaseResponse> getAll(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        return repository.findByActiveTrueOrderByUpdatedAtDesc().stream()
                .filter(a -> q.isBlank()
                        || contains(a.getTitle(), q)
                        || contains(a.getCategory(), q)
                        || contains(a.getSolution(), q)
                        || contains(a.getKeywords(), q))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public KnowledgeBaseResponse getById(UUID id) {
        return toResponse(findEntity(id));
    }

    @Transactional
    public KnowledgeBaseResponse create(KnowledgeBaseRequest request) {
        KnowledgeBaseArticle article = KnowledgeBaseArticle.builder()
                .title(request.getTitle().trim())
                .category(request.getCategory().trim())
                .solution(request.getSolution().trim())
                .keywords(cleanKeywords(request.getKeywords()))
                .answerTemplate(StringUtils.hasText(request.getAnswerTemplate()) ? request.getAnswerTemplate().trim() : request.getSolution().trim())
                .active(request.isActive())
                .build();
        return toResponse(repository.save(article));
    }

    @Transactional
    public KnowledgeBaseResponse update(UUID id, KnowledgeBaseRequest request) {
        KnowledgeBaseArticle article = findEntity(id);
        article.setTitle(request.getTitle().trim());
        article.setCategory(request.getCategory().trim());
        article.setSolution(request.getSolution().trim());
        article.setKeywords(cleanKeywords(request.getKeywords()));
        article.setAnswerTemplate(StringUtils.hasText(request.getAnswerTemplate()) ? request.getAnswerTemplate().trim() : request.getSolution().trim());
        article.setActive(request.isActive());
        return toResponse(repository.save(article));
    }

    @Transactional
    public void delete(UUID id) {
        KnowledgeBaseArticle article = findEntity(id);
        article.setActive(false);
        repository.save(article);
    }

    private KnowledgeBaseArticle findEntity(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Knowledge base article not found"));
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase().contains(q);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    // KAT-125: Duplikate in der komma-separierten Keyword-Liste entfernen (case-insensitive geprüft,
    // ursprüngliche Schreibweise bleibt erhalten, Reihenfolge bleibt erhalten)
    private String cleanKeywords(String value) {
        if (value == null || value.isBlank()) return "";
        Set<String> seenLowerCase = new LinkedHashSet<>();
        List<String> result = new java.util.ArrayList<>();
        for (String raw : value.split(",")) {
            String keyword = raw.trim();
            if (keyword.isEmpty()) continue;
            if (seenLowerCase.add(keyword.toLowerCase())) {
                result.add(keyword);
            }
        }
        return String.join(",", result);
    }

    private KnowledgeBaseResponse toResponse(KnowledgeBaseArticle article) {
        KnowledgeBaseResponse response = new KnowledgeBaseResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setCategory(article.getCategory());
        response.setSolution(article.getSolution());
        response.setKeywords(article.getKeywords());
        response.setAnswerTemplate(article.getAnswerTemplate());
        response.setActive(article.isActive());
        response.setCreatedAt(article.getCreatedAt());
        response.setUpdatedAt(article.getUpdatedAt());
        return response;
    }
}
