package com.springai.chatbot.repository;

import com.springai.chatbot.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversation_IdOrderByCreatedAtAsc(
            java.util.UUID conversationId
    );
}