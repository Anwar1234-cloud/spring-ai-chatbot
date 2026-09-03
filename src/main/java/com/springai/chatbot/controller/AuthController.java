package com.springai.chatbot.security;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class AuthController {

    @GetMapping("/api/auth/me")
    public Map<String, Object> getCurrentUser(
            @AuthenticationPrincipal OAuth2User user) {

        if (user == null) {
            return Map.of(
                    "authenticated", false
            );
        }

        return Map.of(
                "authenticated", true,
                "name", getAttribute(user, "name"),
                "email", getAttribute(user, "email"),
                "picture", getAttribute(user, "picture")
        );
    }

    @PostMapping("/api/auth/logout")
    public Map<String, Object> logout(
            HttpServletRequest request) {

        request.getSession().invalidate();

        return Map.of(
                "success", true,
                "message", "Logged out successfully"
        );
    }

    private String getAttribute(
            OAuth2User user,
            String attribute) {

        Object value = user.getAttribute(attribute);

        return value != null
                ? value.toString()
                : "";
    }
}