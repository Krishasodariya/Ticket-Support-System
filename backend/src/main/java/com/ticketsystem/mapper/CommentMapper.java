package com.ticketsystem.mapper;

import com.ticketsystem.dto.response.CommentResponse;
import com.ticketsystem.model.Comment;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {
    public CommentResponse toResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setAuthorUsername(comment.getAuthor().getUsername());
        response.setInternal(comment.isInternal());
        response.setCreatedAt(comment.getCreatedAt());
        return response;
    }
}
