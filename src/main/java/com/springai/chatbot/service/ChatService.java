package com.springai.chatbot.service;

import com.springai.chatbot.entity.ChatMessage;
import com.springai.chatbot.entity.Conversation;
import com.springai.chatbot.repository.ChatMessageRepository;
import com.springai.chatbot.repository.ConversationRepository;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository
    ) {
        this.chatClient = chatClientBuilder.build();
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public ChatResult chat(String message, String conversationId) {

        // 1. Find existing conversation or create a new one
        Conversation conversation;

        if (conversationId == null || conversationId.isBlank()) {

            conversation = conversationRepository.save(
                    Conversation.builder().build()
            );

        } else {

            UUID id;

            try {
                id = UUID.fromString(conversationId);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid conversationId: " + conversationId
                );
            }

            conversation = conversationRepository
                    .findById(id)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Conversation not found: " + conversationId
                            )
                    );
        }

        // 2. Load previous messages
        List<ChatMessage> previousMessages =
                chatMessageRepository
                        .findByConversation_IdOrderByIdAsc(conversation.getId());

        // 3. Convert database messages to Spring AI messages
        List<org.springframework.ai.chat.messages.Message> aiMessages =
                new ArrayList<>();

        for (ChatMessage chatMessage : previousMessages) {

            if (chatMessage.getRole() == ChatMessage.Role.USER) {

                aiMessages.add(
                        new UserMessage(chatMessage.getContent())
                );

            } else {

                aiMessages.add(
                        new AssistantMessage(chatMessage.getContent())
                );
            }
        }

        // 4. Send conversation + new user message to AI
        String aiResponse;

        if (aiMessages.isEmpty()) {

            aiResponse = chatClient
                    .prompt()
                    .user(message)
                    .call()
                    .content();

        } else {

            aiResponse = chatClient
                    .prompt()
                    .messages(aiMessages)
                    .user(message)
                    .call()
                    .content();
        }

        // 5. Save user message
        ChatMessage userMessage = ChatMessage.builder()
                .conversation(conversation)
                .role(ChatMessage.Role.USER)
                .content(message)
                .build();

        chatMessageRepository.save(userMessage);

        // 6. Save assistant message
        ChatMessage assistantMessage = ChatMessage.builder()
                .conversation(conversation)
                .role(ChatMessage.Role.ASSISTANT)
                .content(aiResponse)
                .build();

        chatMessageRepository.save(assistantMessage);

        // 7. Return response
        return new ChatResult(
                conversation.getId().toString(),
                aiResponse
        );
    }

    public record ChatResult(
            String conversationId,
            String response
    ) {
    }
}
