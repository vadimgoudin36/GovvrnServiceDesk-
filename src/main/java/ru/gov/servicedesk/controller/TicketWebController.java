package ru.gov.servicedesk.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.gov.servicedesk.model.Attachment;
import ru.gov.servicedesk.model.Priority;
import ru.gov.servicedesk.model.Role;
import ru.gov.servicedesk.model.Ticket;
import ru.gov.servicedesk.model.TicketStatus;
import ru.gov.servicedesk.model.User;
import ru.gov.servicedesk.repository.WebAttachmentRepository;
import ru.gov.servicedesk.repository.WebCategoryRepository;
import ru.gov.servicedesk.repository.WebCommentRepository;
import ru.gov.servicedesk.repository.WebTicketRepository;
import ru.gov.servicedesk.repository.WebUserRepository;
import ru.gov.servicedesk.service.AttachmentStorageService;
import ru.gov.servicedesk.service.WebCurrentUserService;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Controller
public class TicketWebController {
    private final WebTicketRepository ticketRepository;
    private final WebCommentRepository commentRepository;
    private final WebCategoryRepository categoryRepository;
    private final WebUserRepository userRepository;
    private final WebAttachmentRepository attachmentRepository;
    private final AttachmentStorageService attachmentStorageService;
    private final WebCurrentUserService currentUserService;

    public TicketWebController(WebTicketRepository ticketRepository,
                               WebCommentRepository commentRepository,
                               WebCategoryRepository categoryRepository,
                               WebUserRepository userRepository,
                               WebAttachmentRepository attachmentRepository,
                               AttachmentStorageService attachmentStorageService,
                               WebCurrentUserService currentUserService) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.attachmentRepository = attachmentRepository;
        this.attachmentStorageService = attachmentStorageService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/tickets";
    }

    @GetMapping("/tickets")
    public String tickets(@RequestParam(required = false) String q, Model model, Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        List<Ticket> tickets = switch (user.getRole()) {
            case EMPLOYEE -> ticketRepository.findByAuthor(user.getId());
            case SPECIALIST, ADMIN -> ticketRepository.search(q);
        };
        model.addAttribute("currentUser", user);
        model.addAttribute("tickets", tickets);
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("priorities", Priority.values());
        return "tickets/list";
    }

    @GetMapping("/tickets/new")
    public String newTicket(Model model, Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        model.addAttribute("currentUser", user);
        model.addAttribute("categories", categoryRepository.findAllNames());
        model.addAttribute("priorities", Priority.values());
        return "tickets/new";
    }

    @PostMapping("/tickets")
    public String createTicket(@RequestParam String title,
                               @RequestParam String description,
                               @RequestParam String category,
                               @RequestParam(defaultValue = "NORMAL") Priority priority,
                               @RequestParam(required = false) MultipartFile file,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) throws IOException {
        User user = currentUserService.requireUser(authentication);
        int ticketId = ticketRepository.create(title.trim(), description.trim(), category, priority, user.getId());
        attachmentStorageService.store(ticketId, user.getId(), file);
        redirectAttributes.addFlashAttribute("message", "Заявка создана");
        return "redirect:/tickets/" + ticketId;
    }

    @GetMapping("/tickets/{id}")
    public String ticketDetails(@PathVariable int id, Model model, Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        Ticket ticket = requireVisibleTicket(id, user);
        model.addAttribute("currentUser", user);
        model.addAttribute("ticket", ticket);
        model.addAttribute("comments", commentRepository.findByTicket(id));
        model.addAttribute("attachments", attachmentRepository.findByTicket(id));
        model.addAttribute("specialists", userRepository.findByRole(Role.SPECIALIST));
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("priorities", Priority.values());
        return "tickets/details";
    }

    @PostMapping("/tickets/{id}/comments")
    public String addComment(@PathVariable int id,
                             @RequestParam String text,
                             @RequestParam(required = false) MultipartFile file,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) throws IOException {
        User user = currentUserService.requireUser(authentication);
        requireVisibleTicket(id, user);
        if (!text.isBlank()) {
            commentRepository.create(id, user.getId(), text.trim());
        }
        attachmentStorageService.store(id, user.getId(), file);
        redirectAttributes.addFlashAttribute("message", "Комментарий добавлен");
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/assign")
    public String assign(@PathVariable int id,
                         @RequestParam int executorId,
                         @RequestParam(required = false) String dueAt,
                         @RequestParam(defaultValue = "NORMAL") Priority priority,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireUser(authentication);
        requireRole(user, Role.ADMIN);
        ticketRepository.assign(id, executorId, dueAt, priority);
        redirectAttributes.addFlashAttribute("message", "Заявка назначена");
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/take")
    public String take(@PathVariable int id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireUser(authentication);
        requireRole(user, Role.SPECIALIST);
        ticketRepository.takeToWork(id, user.getId());
        redirectAttributes.addFlashAttribute("message", "Заявка взята в работу");
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/status")
    public String updateStatus(@PathVariable int id,
                               @RequestParam TicketStatus status,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireUser(authentication);
        requireRole(user, Role.SPECIALIST);
        ticketRepository.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("message", "Статус обновлен");
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/tickets/{id}/complete")
    public String complete(@PathVariable int id,
                           @RequestParam String report,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        User user = currentUserService.requireUser(authentication);
        requireRole(user, Role.SPECIALIST);
        ticketRepository.completeWithReport(id, report.trim());
        redirectAttributes.addFlashAttribute("message", "Отчет сохранен, заявка отмечена выполненной");
        return "redirect:/tickets/" + id;
    }

    @GetMapping("/attachments/{id}")
    public ResponseEntity<InputStreamResource> attachment(@PathVariable int id, Authentication authentication) throws IOException {
        User user = currentUserService.requireUser(authentication);
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Файл не найден"));
        requireVisibleTicket(attachment.getTicketId(), user);
        var path = attachmentStorageService.resolve(attachment);
        MediaType mediaType = attachment.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(attachment.getContentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName().replace("\"", "") + "\"")
                .contentLength(Files.size(path))
                .body(new InputStreamResource(Files.newInputStream(path)));
    }

    private Ticket requireVisibleTicket(int id, User user) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));
        if (user.getRole() == Role.EMPLOYEE && ticket.getAuthorId() != user.getId()) {
            throw new AccessDeniedException("Нет доступа к заявке");
        }
        return ticket;
    }

    private void requireRole(User user, Role role) {
        if (user.getRole() != role) {
            throw new AccessDeniedException("Недостаточно прав");
        }
    }
}
