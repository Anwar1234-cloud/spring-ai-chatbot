package com.springai.chatbot.controller;

import com.springai.chatbot.dto.ConversationResponse;
import com.springai.chatbot.dto.MessageResponse;
import com.springai.chatbot.entity.ChatMessage;
import com.springai.chatbot.entity.Conversation;
import com.springai.chatbot.repository.ChatMessageRepository;
import com.springai.chatbot.repository.ConversationRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ConversationController(
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    // API 1:
    // GET /api/conversations
    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getAllConversations() {

        List<ConversationResponse> conversations =
                conversationRepository.findAll()
                        .stream()
                        .map(this::toConversationResponse)
                        .toList();

        return ResponseEntity.ok(conversations);
    }

    // API 2:
    // GET /api/conversations/{conversationId}/messages
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable UUID conversationId
    ) {

        if (!conversationRepository.existsById(conversationId)) {
            return ResponseEntity.notFound().build();
        }

        List<MessageResponse> messages =
                chatMessageRepository
                        .findByConversation_IdOrderByCreatedAtAsc(conversationId)
                        .stream()
                        .map(message -> MessageResponse.builder()
                                .id(message.getId())
                                .role(message.getRole().name())
                                .content(message.getContent())
                                .createdAt(message.getCreatedAt())
                                .build())
                        .toList();

        return ResponseEntity.ok(messages);
    }

    private ConversationResponse toConversationResponse(
            Conversation conversation
    ) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private MessageResponse toMessageResponse(
            ChatMessage message
    ) {
        return MessageResponse.builder()
                .id(message.getId())
                .role(message.getRole().name())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable UUID conversationId
    ) {

        if (!conversationRepository.existsById(conversationId)) {
            return ResponseEntity.notFound().build();
        }

        conversationRepository.deleteById(conversationId);

        return ResponseEntity.noContent().build();
    }
}
