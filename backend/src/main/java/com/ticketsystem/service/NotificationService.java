package com.ticketsystem.service;

import com.ticketsystem.dto.response.NotificationResponse;
import com.ticketsystem.exception.ResourceNotFoundException;
import com.ticketsystem.model.Notification;
import com.ticketsystem.model.Ticket;
import com.ticketsystem.model.User;
import com.ticketsystem.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public NotificationService(NotificationRepository notificationRepository, UserService userService) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
    }

    public void notifyUser(User recipient, Ticket ticket, String title, String message) {
        if (recipient == null) return;
        Notification notification = Notification.builder()
                .recipient(recipient)
                .ticket(ticket)
                .title(title)
                .message(message)
                .read(false)
                .build();
        notificationRepository.save(notification);
    }


    public void notifySimulatedEmail(User recipient, Ticket ticket, String subject, String message) {
        if (recipient == null) return;
        notifyUser(recipient, ticket, "E-Mail simuliert: " + subject, "An " + recipient.getEmail() + ": " + message);
    }

    public List<NotificationResponse> getMyNotifications(String username) {
        User user = userService.findUserEntityByUsername(username);
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public long countUnread(String username) {
        User user = userService.findUserEntityByUsername(username);
        return notificationRepository.countByRecipientAndReadFalse(user);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID id, String username) {
        User user = userService.findUserEntityByUsername(username);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification not found");
        }
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    // [Nzchupa | 2026-06-26] KAT-91: Einzelne Benachrichtigung löschen
    // Deletes a single notification — only the recipient may delete their own notification
    @Transactional
    public void deleteNotification(UUID id, String username) {
        User user = userService.findUserEntityByUsername(username);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification not found");
        }
        notificationRepository.delete(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setRead(notification.isRead());
        response.setCreatedAt(notification.getCreatedAt());
        if (notification.getTicket() != null) {
            response.setTicketId(notification.getTicket().getId());
        }
        return response;
    }
}
