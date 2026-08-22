package com.springai.chatbot.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    private String message;

    private String conversationId;
}