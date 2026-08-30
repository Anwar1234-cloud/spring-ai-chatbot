package com.springai.chatbot.controller;

import com.springai.chatbot.dto.ChatRequest;
import com.springai.chatbot.dto.ChatResponse;
import com.springai.chatbot.dto.FeedbackRequest;
import com.springai.chatbot.service.ChatService;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // ============================================================
    // NORMAL TEXT CHAT
    // Content-Type: application/json
    // ============================================================

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
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


    // ============================================================
    // CHAT WITH PDF / IMAGE
    // Content-Type: multipart/form-data
    // ============================================================

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ChatResponse> chatWithFile(
            @RequestParam(
                    value = "message",
                    required = false,
                    defaultValue = ""
            )
            String message,

            @RequestParam(
                    value = "conversationId",
                    required = false
            )
            String conversationId,

            @RequestParam(
                    value = "file",
                    required = false
            )
            MultipartFile file
    ) {

        ChatService.ChatResult result =
                chatService.chat(
                        message,
                        conversationId,
                        file
                );

        ChatResponse response = new ChatResponse(
                result.conversationId(),
                result.response()
        );

        return ResponseEntity.ok(response);
    }


    // ============================================================
    // REGENERATE RESPONSE
    // ============================================================

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


    // ============================================================
    // FEEDBACK
    // ============================================================

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