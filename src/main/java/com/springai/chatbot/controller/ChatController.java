package com.springai.chatbot.controller;

import com.springai.chatbot.dto.ChatRequest;
import com.springai.chatbot.dto.ChatResponse;
import com.springai.chatbot.service.ChatService;

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
    public ResponseEntity<ChatResponse> regenerate(
            @RequestParam String conversationId,
            @RequestParam Long messageId
    ) {

        ChatService.ChatResult result =
                chatService.regenerate(
                        conversationId,
                        messageId
                );

        return ResponseEntity.ok(
                new ChatResponse(
                        result.conversationId(),
                        result.response()
                )
        );
    }
}