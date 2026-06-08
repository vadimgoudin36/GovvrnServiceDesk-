package ru.gov.servicedesk.controller;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.gov.servicedesk.model.Priority;
import ru.gov.servicedesk.model.Role;
import ru.gov.servicedesk.model.TicketStatus;
import ru.gov.servicedesk.model.User;
import ru.gov.servicedesk.repository.WebTicketRepository;
import ru.gov.servicedesk.repository.WebUserRepository;
import ru.gov.servicedesk.service.WebCurrentUserService;

@Controller
public class AdminWebController {
    private final WebUserRepository userRepository;
    private final WebTicketRepository ticketRepository;
    private final WebCurrentUserService currentUserService;

    public AdminWebController(WebUserRepository userRepository, WebTicketRepository ticketRepository, WebCurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/admin/users")
    public String users(Model model, Authentication authentication) {
        User user = requireAdmin(authentication);
        model.addAttribute("currentUser", user);
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @GetMapping("/admin/assignments")
    public String assignments(Model model, Authentication authentication) {
        User user = requireAdmin(authentication);
        model.addAttribute("currentUser", user);
        model.addAttribute("tickets", ticketRepository.findAll());
        model.addAttribute("specialists", userRepository.findByRole(Role.SPECIALIST));
        model.addAttribute("priorities", Priority.values());
        return "admin/assignments";
    }

    @PostMapping("/admin/assignments")
    public String assignTicket(@RequestParam int ticketId,
                               @RequestParam int executorId,
                               @RequestParam(required = false) String dueAt,
                               @RequestParam(defaultValue = "NORMAL") Priority priority,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        requireAdmin(authentication);
        ticketRepository.assign(ticketId, executorId, dueAt, priority);
        redirectAttributes.addFlashAttribute("message", "Заявка назначена");
        return "redirect:/admin/assignments";
    }

    @PostMapping("/admin/users")
    public String createUser(@RequestParam String login,
                             @RequestParam String password,
                             @RequestParam String fullName,
                             @RequestParam Role role,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        requireAdmin(authentication);
        userRepository.create(login.trim(), password.trim(), fullName.trim(), role);
        redirectAttributes.addFlashAttribute("message", "Пользователь добавлен");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/update")
    public String updateUser(@RequestParam int id,
                             @RequestParam String login,
                             @RequestParam(required = false) String password,
                             @RequestParam String fullName,
                             @RequestParam Role role,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        requireAdmin(authentication);
        User existing = userRepository.findById(id).orElseThrow();
        String actualPassword = password == null || password.isBlank() ? existing.getPassword() : password.trim();
        userRepository.update(id, login.trim(), actualPassword, fullName.trim(), role);
        redirectAttributes.addFlashAttribute("message", "Пользователь обновлен");
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/stats")
    public String stats(Model model, Authentication authentication) {
        User user = requireAdmin(authentication);
        model.addAttribute("currentUser", user);
        model.addAttribute("statusStats", ticketRepository.countByStatus());
        model.addAttribute("priorityStats", ticketRepository.countByPriority());
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("priorities", Priority.values());
        return "admin/stats";
    }

    private User requireAdmin(Authentication authentication) {
        User user = currentUserService.requireUser(authentication);
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Недостаточно прав");
        }
        return user;
    }
}
