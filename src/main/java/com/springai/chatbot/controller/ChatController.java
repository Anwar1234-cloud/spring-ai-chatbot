package com.springai.chatbot.controller;

import com.springai.chatbot.dto.ChatRequest;
import com.springai.chatbot.dto.ChatResponse;
import com.springai.chatbot.dto.FeedbackRequest;
import com.springai.chatbot.service.ChatService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request
    ) {

        ChatService.ChatResult result =
                chatService.chat(
                        request.getMessage(),
                        request.getConversationId()
                );

        ChatResponse response = new ChatResponse(
                result.conversationId(),
                result.response()
        );

        return ResponseEntity.ok(response);
    }
    @PostMapping("/regenerate")
    public ChatService.ChatResult regenerate(
            @RequestParam String conversationId,
            @RequestParam Long messageId
    ) {

        return chatService.regenerate(
                conversationId,
                messageId
        );
    }
    @PostMapping("/feedback")
    public void saveFeedback(
            @Valid @RequestBody FeedbackRequest request
    ) {

        chatService.saveFeedback(
                request.messageId(),
                request.type()
        );
    }
}