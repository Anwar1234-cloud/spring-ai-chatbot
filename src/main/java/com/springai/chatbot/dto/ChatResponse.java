package com.springai.chatbot.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChatResponse {
    private String message;
    private String conversationId;
    private boolean success;
    private String error;
}