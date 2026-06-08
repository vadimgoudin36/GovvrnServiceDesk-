package ru.gov.servicedesk.service;

import ru.gov.servicedesk.dao.CommentDao;
import ru.gov.servicedesk.dao.TicketDao;
import ru.gov.servicedesk.model.Role;
import ru.gov.servicedesk.model.Ticket;
import ru.gov.servicedesk.model.TicketStatus;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class RuleEngineService {
    private static final DateTimeFormatter SQLITE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TicketDao ticketDao = new TicketDao();
    private final CommentDao commentDao = new CommentDao();

    public void applyAutomaticRules() throws SQLException {
        for (Ticket ticket : ticketDao.findAll()) {
            applyAutomaticRulesForTicket(ticket.getId());
        }
    }

    public void applyAutomaticRulesForTicket(int ticketId) throws SQLException {
        Ticket ticket = ticketDao.findById(ticketId);
        if (ticket == null || ticket.getStatus() == TicketStatus.CLOSED) {
            return;
        }

        applyUrgentRule(ticket);
        ticket = ticketDao.findById(ticketId);
        applySlaRule(ticket);
        ticket = ticketDao.findById(ticketId);
        applyEscalationRule(ticket);
        ticket = ticketDao.findById(ticketId);
        applyAutoCloseRule(ticket);
    }

    private void applyUrgentRule(Ticket ticket) throws SQLException {
        if (ticket == null || ticket.getStatus() == TicketStatus.IN_PROGRESS || ticket.getStatus() == TicketStatus.DONE || ticket.getStatus() == TicketStatus.CLOSED) {
            return;
        }
        if (containsAny(ticket.getDescription(), "срочно", "не работает", "критично")) {
            ticketDao.updateStatusAutomatically(
                    ticket.getId(),
                    TicketStatus.URGENT_REVIEW,
                    "FR-05.2: в описании заявки найдены слова срочности"
            );
        }
    }

    private void applySlaRule(Ticket ticket) throws SQLException {
        if (ticket == null || ticket.getStatus() != TicketStatus.NEW) {
            return;
        }
        LocalDateTime createdAt = parseDateTime(ticket.getCreatedAt());
        if (createdAt != null && createdAt.plusHours(2).isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            ticketDao.updateStatusAutomatically(
                    ticket.getId(),
                    TicketStatus.OVERDUE,
                    "FR-05.1: заявка находится в статусе \"Новая\" больше 2 часов"
            );
        }
    }

    private void applyEscalationRule(Ticket ticket) throws SQLException {
        if (ticket == null || ticket.getStatus() == TicketStatus.IN_PROGRESS || ticket.getStatus() == TicketStatus.DONE || ticket.getStatus() == TicketStatus.CLOSED) {
            return;
        }
        if (commentDao.countByTicket(ticket.getId()) >= 3 && !ticketDao.hasStatusChangeLogs(ticket.getId())) {
            ticketDao.updateStatusAutomatically(
                    ticket.getId(),
                    TicketStatus.ESCALATION,
                    "FR-05.3: по заявке оставлено 3+ комментария, статус ни разу не менялся"
            );
        }
    }

    private void applyAutoCloseRule(Ticket ticket) throws SQLException {
        if (ticket == null || ticket.getStatus() == TicketStatus.CLOSED) {
            return;
        }
        if (ticket.getStatus() != TicketStatus.DONE
                && commentDao.hasCommentByRoleContaining(ticket.getId(), Role.SPECIALIST, "исправлено", "готово")) {
            ticketDao.updateStatusAutomatically(
                    ticket.getId(),
                    TicketStatus.DONE,
                    "FR-05.4: IT-специалист сообщил об исправлении"
            );
            ticket = ticketDao.findById(ticket.getId());
        }
        if (ticket != null && ticket.getStatus() == TicketStatus.DONE
                && commentDao.hasCommentByAuthorContaining(ticket.getId(), ticket.getAuthorId(), "спасибо")) {
            ticketDao.updateStatusAutomatically(
                    ticket.getId(),
                    TicketStatus.CLOSED,
                    "FR-05.4: автор заявки подтвердил выполнение словом \"спасибо\""
            );
        }
    }

    private boolean containsAny(String text, String... words) {
        if (text == null) {
            return false;
        }
        String lowerText = text.toLowerCase();
        for (String word : words) {
            if (lowerText.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, SQLITE_DATE_TIME);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(value.replace(" ", "T"));
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }
}
