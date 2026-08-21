package com.springai.chatbot.service;

import com.springai.chatbot.dto.Message;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String chat(String message, List<Message> history) {

        var prompt = chatClient.prompt();

        if (history != null && !history.isEmpty()) {

            for (Message msg : history) {

                if ("user".equalsIgnoreCase(msg.getRole())) {
                    prompt.messages(new UserMessage(msg.getContent()));

                } else if ("assistant".equalsIgnoreCase(msg.getRole())) {
                    prompt.messages(new AssistantMessage(msg.getContent()));
                }
            }
        }

        return prompt
                .user(message)
                .call()
                .content();
    }
}
