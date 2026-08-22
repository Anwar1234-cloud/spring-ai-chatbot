package com.springai.chatbot.repository;

import com.springai.chatbot.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversation_IdOrderByIdAsc(UUID conversationId);
}
