package com.ticketsystem.service;

import com.ticketsystem.dto.request.CreateCommentRequest;
import com.ticketsystem.dto.response.CommentResponse;
import com.ticketsystem.exception.TicketNotFoundException;
import com.ticketsystem.mapper.CommentMapper;
import com.ticketsystem.model.Comment;
import com.ticketsystem.model.Ticket;
import com.ticketsystem.model.User;
import com.ticketsystem.model.enums.UserRole;
import com.ticketsystem.repository.CommentRepository;
import com.ticketsystem.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserService userService;
    private final CommentMapper commentMapper;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository, TicketRepository ticketRepository,
                          UserService userService, CommentMapper commentMapper, NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.userService = userService;
        this.commentMapper = commentMapper;
        this.notificationService = notificationService;
    }

    public List<CommentResponse> getCommentsForTicket(UUID ticketId, String username) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new TicketNotFoundException("Ticket not found"));
        User user = userService.findUserEntityByUsername(username);

        List<Comment> comments;
        if (user.getRole() == UserRole.CUSTOMER) {
            comments = commentRepository.findByTicketAndIsInternalFalseOrderByCreatedAtAsc(ticket);
        } else {
            comments = commentRepository.findByTicketOrderByCreatedAtAsc(ticket);
        }
        return comments.stream().map(commentMapper::toResponse).collect(Collectors.toList());
    }

    public CommentResponse createComment(UUID ticketId, CreateCommentRequest request, String username) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new TicketNotFoundException("Ticket not found"));
        User user = userService.findUserEntityByUsername(username);

        boolean isInternal = request.isInternal() && user.getRole() != UserRole.CUSTOMER;

        Comment comment = Comment.builder()
                .ticket(ticket)
                .author(user)
                .content(request.getContent().trim())
                .isInternal(isInternal)
                .build();

        Comment saved = commentRepository.save(comment);
        if (!isInternal) {
            if (!ticket.getCreatedBy().getId().equals(user.getId())) {
                notificationService.notifyUser(ticket.getCreatedBy(), ticket, "Neuer Kommentar", "Es gibt einen neuen Kommentar zu deinem Ticket '" + ticket.getTitle() + "'.");
            }
            if (ticket.getAssignedTo() != null && !ticket.getAssignedTo().getId().equals(user.getId())) {
                notificationService.notifyUser(ticket.getAssignedTo(), ticket, "Neuer Kommentar", "Es gibt einen neuen Kommentar im Ticket '" + ticket.getTitle() + "'.");
            }
        }
        return commentMapper.toResponse(saved);
    }
}
