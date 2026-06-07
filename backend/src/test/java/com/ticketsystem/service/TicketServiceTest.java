package com.ticketsystem.service;

import com.ticketsystem.dto.request.CreateTicketRequest;
import com.ticketsystem.dto.request.UpdateTicketRequest;
import com.ticketsystem.exception.InvalidStatusTransitionException;
import com.ticketsystem.mapper.TicketMapper;
import com.ticketsystem.model.Category;
import com.ticketsystem.model.Ticket;
import com.ticketsystem.model.User;
import com.ticketsystem.model.enums.TicketPriority;
import com.ticketsystem.model.enums.TicketStatus;
import com.ticketsystem.model.enums.UserRole;
import com.ticketsystem.repository.AuditLogRepository;
import com.ticketsystem.repository.CategoryRepository;
import com.ticketsystem.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private UserService userService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TicketService ticketService;

    private User testUser;
    private Ticket testTicket;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setRole(UserRole.AGENT);

        testTicket = new Ticket();
        testTicket.setId(UUID.randomUUID());
        testTicket.setTitle("Test Ticket");
        testTicket.setStatus(TicketStatus.OPEN);
        testTicket.setCreatedBy(testUser);
    }

    @Test
    void createTicket_success() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("New");
        request.setDescription("Desc");
        request.setPriority(TicketPriority.HIGH);

        when(userService.findUserEntityByUsername("testuser")).thenReturn(testUser);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        ticketService.createTicket(request, "testuser");

        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    void updateTicketStatus_validTransition_success() {
        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setStatus(TicketStatus.IN_PROGRESS);

        when(ticketRepository.findById(testTicket.getId())).thenReturn(Optional.of(testTicket));
        when(userService.findUserEntityByUsername("testuser")).thenReturn(testUser);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        ticketService.updateTicket(testTicket.getId(), request, "testuser");

        assertEquals(TicketStatus.IN_PROGRESS, testTicket.getStatus());
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    void updateTicketStatus_invalidTransition_throwsException() {
        testTicket.setStatus(TicketStatus.CLOSED);
        
        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setStatus(TicketStatus.IN_PROGRESS);

        when(ticketRepository.findById(testTicket.getId())).thenReturn(Optional.of(testTicket));
        when(userService.findUserEntityByUsername("testuser")).thenReturn(testUser);

        assertThrows(InvalidStatusTransitionException.class, () -> {
            ticketService.updateTicket(testTicket.getId(), request, "testuser");
        });
    }
}
