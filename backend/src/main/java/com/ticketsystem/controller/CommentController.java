package com.ticketsystem.controller;

import com.ticketsystem.dto.request.CreateCommentRequest;
import com.ticketsystem.dto.response.CommentResponse;
import com.ticketsystem.service.CommentService;
import com.ticketsystem.service.RealtimeEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
public class CommentController {

    private final CommentService commentService;
    private final RealtimeEventService realtimeEventService;

    public CommentController(CommentService commentService, RealtimeEventService realtimeEventService) {
        this.commentService = commentService;
        this.realtimeEventService = realtimeEventService;
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable UUID ticketId, Authentication authentication) {
        return ResponseEntity.ok(commentService.getCommentsForTicket(ticketId, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@PathVariable UUID ticketId, 
                                                         @Valid @RequestBody CreateCommentRequest request, 
                                                         Authentication authentication) {
        CommentResponse createdComment = commentService.createComment(ticketId, request, authentication.getName());
        realtimeEventService.publishCommentCreated(ticketId, createdComment.getId());
        return ResponseEntity.ok(createdComment);
    }
}
