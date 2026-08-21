package com.springai.chatbot.controller;

import com.springai.chatbot.dto.ChatRequest;
import com.springai.chatbot.dto.ChatResponse;
import com.springai.chatbot.service.ChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {

        String response = chatService.chat(
                request.getMessage(),
                request.getHistory()
        );

        return new ChatResponse(response);
    }
}