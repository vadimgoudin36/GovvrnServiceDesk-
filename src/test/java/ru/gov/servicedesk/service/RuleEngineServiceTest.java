package ru.gov.servicedesk.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.gov.servicedesk.dao.CommentDao;
import ru.gov.servicedesk.dao.TicketDao;
import ru.gov.servicedesk.model.Role;
import ru.gov.servicedesk.model.Ticket;
import ru.gov.servicedesk.model.TicketStatus;

import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты автоматической смены статусов по правилам FR-05.
 */
class RuleEngineServiceTest {
    private TicketDao ticketDao;
    private CommentDao commentDao;
    private RuleEngineService ruleEngine;

    @BeforeEach
    void setUp() {
        ticketDao = mock(TicketDao.class);
        commentDao = mock(CommentDao.class);
        ruleEngine = new RuleEngineService(ticketDao, commentDao);
    }

    @Test
    @DisplayName("Срочное слово переводит новую заявку на срочное рассмотрение")
    void urgentDescriptionChangesStatus() throws SQLException {
        Ticket initial = ticket(1, TicketStatus.NEW, "Критично: не работает сеть", "2026-06-09 10:30:00");
        Ticket urgent = ticket(1, TicketStatus.URGENT_REVIEW, initial.getDescription(), initial.getCreatedAt());
        when(ticketDao.findById(1)).thenReturn(initial, urgent, urgent, urgent);

        ruleEngine.applyAutomaticRulesForTicket(1);

        verify(ticketDao).updateStatusAutomatically(
                1,
                TicketStatus.URGENT_REVIEW,
                "FR-05.2: в описании заявки найдены слова срочности"
        );
    }

    @Test
    @DisplayName("Новая заявка старше двух часов становится просроченной")
    void oldNewTicketBecomesOverdue() throws SQLException {
        Ticket ticket = ticket(2, TicketStatus.NEW, "Плановая заявка", "2020-01-01 00:00:00");
        Ticket overdue = ticket(2, TicketStatus.OVERDUE, ticket.getDescription(), ticket.getCreatedAt());
        when(ticketDao.findById(2)).thenReturn(ticket, ticket, overdue, overdue);

        ruleEngine.applyAutomaticRulesForTicket(2);

        verify(ticketDao).updateStatusAutomatically(
                2,
                TicketStatus.OVERDUE,
                "FR-05.1: заявка находится в статусе \"Новая\" больше 2 часов"
        );
    }

    @Test
    @DisplayName("Три комментария без смены статуса вызывают эскалацию")
    void threeCommentsCauseEscalation() throws SQLException {
        Ticket ticket = ticket(3, TicketStatus.URGENT_REVIEW, "Требуется помощь", "2026-06-09 10:30:00");
        Ticket escalated = ticket(3, TicketStatus.ESCALATION, ticket.getDescription(), ticket.getCreatedAt());
        when(ticketDao.findById(3)).thenReturn(ticket, ticket, ticket, escalated);
        when(commentDao.countByTicket(3)).thenReturn(3);
        when(ticketDao.hasStatusChangeLogs(3)).thenReturn(false);

        ruleEngine.applyAutomaticRulesForTicket(3);

        verify(ticketDao).updateStatusAutomatically(
                3,
                TicketStatus.ESCALATION,
                "FR-05.3: по заявке оставлено 3+ комментария, статус ни разу не менялся"
        );
    }

    @Test
    @DisplayName("Сообщение специалиста об исправлении завершает заявку")
    void specialistReadyCommentCompletesTicket() throws SQLException {
        Ticket ticket = ticket(4, TicketStatus.IN_PROGRESS, "Настроить рабочее место", "2026-06-09 10:30:00");
        Ticket done = ticket(4, TicketStatus.DONE, ticket.getDescription(), ticket.getCreatedAt());
        when(ticketDao.findById(4)).thenReturn(ticket, ticket, ticket, ticket, done);
        when(commentDao.hasCommentByRoleContaining(4, Role.SPECIALIST, "исправлено", "готово"))
                .thenReturn(true);

        ruleEngine.applyAutomaticRulesForTicket(4);

        verify(ticketDao).updateStatusAutomatically(
                4,
                TicketStatus.DONE,
                "FR-05.4: IT-специалист сообщил об исправлении"
        );
    }

    @Test
    @DisplayName("Благодарность автора закрывает выполненную заявку")
    void authorThanksClosesDoneTicket() throws SQLException {
        Ticket ticket = ticket(5, TicketStatus.DONE, "Настроить почту", "2026-06-09 10:30:00");
        when(ticketDao.findById(5)).thenReturn(ticket, ticket, ticket, ticket);
        when(commentDao.hasCommentByAuthorContaining(5, 10, "спасибо")).thenReturn(true);

        ruleEngine.applyAutomaticRulesForTicket(5);

        verify(ticketDao).updateStatusAutomatically(
                5,
                TicketStatus.CLOSED,
                "FR-05.4: автор заявки подтвердил выполнение словом \"спасибо\""
        );
    }

    @Test
    @DisplayName("Закрытая заявка не обрабатывается повторно")
    void closedTicketIsIgnored() throws SQLException {
        when(ticketDao.findById(6))
                .thenReturn(ticket(6, TicketStatus.CLOSED, "Закрытая заявка", "2026-06-09 10:30:00"));

        ruleEngine.applyAutomaticRulesForTicket(6);

        verify(ticketDao, never()).updateStatusAutomatically(
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(),
                contains("FR-05")
        );
    }

    private Ticket ticket(int id, TicketStatus status, String description, String createdAt) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setStatus(status);
        ticket.setDescription(description);
        ticket.setCreatedAt(createdAt);
        ticket.setAuthorId(10);
        return ticket;
    }
}
