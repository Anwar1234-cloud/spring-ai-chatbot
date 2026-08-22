package com.springai.chatbot.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Message {
    private String role; 
    private String content;
}