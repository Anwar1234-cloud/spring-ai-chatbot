package com.springai.chatbot.repository;

import com.springai.chatbot.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationRepository
        extends JpaRepository<Conversation, UUID> {
}
