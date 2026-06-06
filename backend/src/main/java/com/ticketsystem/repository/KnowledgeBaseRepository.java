package com.ticketsystem.repository;

import com.ticketsystem.model.KnowledgeBaseArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBaseArticle, UUID> {
    List<KnowledgeBaseArticle> findByActiveTrueOrderByUpdatedAtDesc();
    List<KnowledgeBaseArticle> findByCategoryIgnoreCaseAndActiveTrueOrderByUpdatedAtDesc(String category);
}
