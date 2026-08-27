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
import java.util.stream.Collectors;

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

            String title = message.trim();

            if (title.length() > 50) {
                title = title.substring(0, 50) + "...";
            }

            conversation = conversationRepository.save(
                    Conversation.builder()
                            .title(title)
                            .build()
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

        // REST OF YOUR EXISTING CODE...

        // 2. Load previous messages
        List<ChatMessage> previousMessages =
                chatMessageRepository
                        .findByConversation_IdOrderByCreatedAtAsc(conversation.getId());

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

    private String createConversationTitle(String message) {

        if (message == null || message.isBlank()) {
            return "New Conversation";
        }

        String title = message.trim();

        if (title.length() > 40) {
            title = title.substring(0, 40) + "...";
        }

        return title;
    }

    public record ChatResult(
            String conversationId,
            String response
    ) {
    }

    @Transactional
    public ChatResult regenerate(
            String conversationId,
            Long assistantMessageId
    ) {

        UUID id = UUID.fromString(conversationId);

        Conversation conversation =
                conversationRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Conversation not found"
                                )
                        );

        List<ChatMessage> history =
                chatMessageRepository
                        .findByConversation_IdOrderByCreatedAtAsc(id);

        int assistantIndex = -1;

        for (int i = 0; i < history.size(); i++) {

            if (history.get(i).getId().equals(assistantMessageId)) {
                assistantIndex = i;
                break;
            }
        }

        if (
                assistantIndex <= 0 ||
                        history.get(assistantIndex).getRole()
                                != ChatMessage.Role.ASSISTANT
        ) {
            throw new IllegalArgumentException(
                    "Assistant message not found."
            );
        }

        ChatMessage userMessage = history.get(assistantIndex - 1);

        /*
         * Build conversation context
         * excluding the assistant message
         * being regenerated.
         */
        List<org.springframework.ai.chat.messages.Message> aiMessages =
                history.subList(0, assistantIndex)
                        .stream()
                        .map(message -> {

                            if (
                                    message.getRole()
                                            == ChatMessage.Role.USER
                            ) {

                                return new UserMessage(
                                        message.getContent()
                                );
                            }

                            return new AssistantMessage(
                                    message.getContent()
                            );

                        })
                        .collect(Collectors.toList());

        String newResponse =
                chatClient.prompt()
                        .messages(aiMessages)
                        .call()
                        .content();

        /*
         * Replace old assistant response.
         */
        ChatMessage assistantMessage =
                history.get(assistantIndex);

        assistantMessage.setContent(newResponse);

        chatMessageRepository.save(assistantMessage);

        return new ChatResult(
                conversation.getId().toString(),
                newResponse
        );
    }
}
