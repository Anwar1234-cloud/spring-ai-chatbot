package com.springai.chatbot.dto;

import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ChatRequest {
    private String message;
    private String conversationId;
    private List<Message> history;
}