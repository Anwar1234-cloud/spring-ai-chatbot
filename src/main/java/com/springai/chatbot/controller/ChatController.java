package com.springai.chatbot.controller;

import com.springai.chatbot.dto.ChatRequest;
import com.springai.chatbot.dto.ChatResponse;
import com.springai.chatbot.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    // Regular chat
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request) {
        log.info("Chat request: {}", request.getMessage());
        return ResponseEntity.ok(chatService.chat(request));
    }

    // Simple message endpoint
    @GetMapping("/ask")
    public ResponseEntity<ChatResponse> ask(
            @RequestParam String message) {
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        return ResponseEntity.ok(chatService.chat(request));
    }

    // Health check
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chatbot is running!");
    }
}