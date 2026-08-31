
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

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }


    // ============================================================
    // NORMAL TEXT CHAT
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

        ChatResponse response =
                new ChatResponse(
                        result.conversationId(),
                        result.response(),
                        result.messageId()
                );

        return ResponseEntity.ok(response);
    }


    // ============================================================
    // CHAT WITH PDF / IMAGE
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

        ChatResponse response =
                new ChatResponse(
                        result.conversationId(),
                        result.response(),
                        result.messageId()
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



// ============================================================
// STREAMING CHAT
// ============================================================

    @PostMapping(
            value = "/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public Flux<String> streamChat(
            @RequestBody ChatRequest request
    ) {

        return chatService.streamChat(
                request.getMessage(),
                request.getConversationId()
        );
    }


// ============================================================
// STREAMING CHAT WITH PDF / IMAGE
// ============================================================

    @PostMapping(
            value = "/stream-file",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public Flux<String> streamFileChat(

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

        return chatService.streamChat(
                message,
                conversationId,
                file
        );
    }


}

