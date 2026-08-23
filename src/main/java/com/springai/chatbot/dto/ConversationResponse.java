package com.springai.chatbot.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {

    private UUID id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
