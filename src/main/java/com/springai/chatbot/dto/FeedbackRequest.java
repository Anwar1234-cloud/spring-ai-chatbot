package com.springai.chatbot.dto;

import jakarta.validation.constraints.NotNull;

public record FeedbackRequest(

        @NotNull
        Long messageId,

        @NotNull
        FeedbackType type

) {

    public enum FeedbackType {
        LIKE,
        DISLIKE
    }
}
