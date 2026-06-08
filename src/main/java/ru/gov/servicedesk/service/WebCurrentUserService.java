package ru.gov.servicedesk.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import ru.gov.servicedesk.model.User;
import ru.gov.servicedesk.repository.WebUserRepository;

@Service
public class WebCurrentUserService {
    private final WebUserRepository userRepository;

    public WebCurrentUserService(WebUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireUser(Authentication authentication) {
        return userRepository.findByLogin(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
    }
}
