package com.springai.chatbot.service;

import com.springai.chatbot.entity.User;
import com.springai.chatbot.repository.UserRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User processGoogleUser(OAuth2User oauthUser) {

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String picture = oauthUser.getAttribute("picture");
        String providerId = oauthUser.getAttribute("sub");

        if (email == null) {
            throw new IllegalArgumentException(
                    "Google account email not available"
            );
        }

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseGet(() ->
                                User.builder()
                                        .email(email)
                                        .build()
                        );

        user.setName(
                name != null
                        ? name
                        : email
        );

        user.setPicture(picture);
        user.setProvider("GOOGLE");
        user.setProviderId(providerId);

        return userRepository.save(user);
    }
}
