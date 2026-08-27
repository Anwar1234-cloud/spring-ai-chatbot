package com.springai.chatbot.service;

import com.springai.chatbot.dto.FeedbackRequest;
import com.springai.chatbot.entity.ChatMessage;
import com.springai.chatbot.entity.Conversation;
import com.springai.chatbot.repository.ChatMessageRepository;
import com.springai.chatbot.repository.ConversationRepository;

import com.springai.chatbot.repository.FeedbackRepository;
import com.springai.chatbot.entity.Feedback;
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

    private final FeedbackRepository feedbackRepository;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository,
            FeedbackRepository feedbackRepository
    ) {
        this.chatClient = chatClientBuilder.build();
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.feedbackRepository = feedbackRepository;
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

        UUID conversationUUID;

        try {
            conversationUUID = UUID.fromString(conversationId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid conversationId: " + conversationId
            );
        }

        Conversation conversation =
                conversationRepository.findById(conversationUUID)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Conversation not found: " + conversationId
                                )
                        );

        // Find the assistant message from PostgreSQL
        ChatMessage assistantMessage =
                chatMessageRepository.findById(assistantMessageId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Assistant message not found: "
                                                + assistantMessageId
                                )
                        );

        // Make sure message belongs to this conversation
        if (!assistantMessage.getConversation()
                .getId()
                .equals(conversationUUID)) {

            throw new IllegalArgumentException(
                    "Message does not belong to this conversation"
            );
        }

        if (assistantMessage.getRole() != ChatMessage.Role.ASSISTANT) {

            throw new IllegalArgumentException(
                    "Only assistant messages can be regenerated"
            );
        }

        // Load all messages
        List<ChatMessage> allMessages =
                chatMessageRepository
                        .findByConversation_IdOrderByCreatedAtAsc(
                                conversationUUID
                        );

        // Find position of the assistant message
        int assistantIndex = -1;

        for (int i = 0; i < allMessages.size(); i++) {

            if (allMessages.get(i)
                    .getId()
                    .equals(assistantMessageId)) {

                assistantIndex = i;
                break;
            }
        }

        if (assistantIndex <= 0) {

            throw new IllegalArgumentException(
                    "Unable to find previous user message"
            );
        }

        // Previous message must be USER
        ChatMessage userMessage =
                allMessages.get(assistantIndex - 1);

        if (userMessage.getRole() != ChatMessage.Role.USER) {

            throw new IllegalArgumentException(
                    "Assistant message has no previous user message"
            );
        }

        /*
         * Build AI history BEFORE the assistant response
         */
        List<org.springframework.ai.chat.messages.Message> aiMessages =
                new ArrayList<>();

        for (int i = 0; i < assistantIndex; i++) {

            ChatMessage chatMessage = allMessages.get(i);

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

        /*
         * Remove old assistant response
         */
        chatMessageRepository.delete(assistantMessage);

        /*
         * Generate a new response using the SAME
         * conversation history.
         */
        String newResponse =
                chatClient
                        .prompt()
                        .messages(aiMessages)
                        .call()
                        .content();

        /*
         * Save new assistant response
         */
        ChatMessage newAssistantMessage =
                ChatMessage.builder()
                        .conversation(conversation)
                        .role(ChatMessage.Role.ASSISTANT)
                        .content(newResponse)
                        .build();

        chatMessageRepository.save(newAssistantMessage);

        return new ChatResult(
                conversation.getId().toString(),
                newResponse
        );
    }
    @Transactional
    public void saveFeedback(
            Long messageId,
            FeedbackRequest.FeedbackType type
    ) {

        System.out.println("FEEDBACK MESSAGE ID: " + messageId);
        System.out.println("FEEDBACK TYPE: " + type);

        ChatMessage message =
                chatMessageRepository.findById(messageId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Message not found: " + messageId
                                )
                        );

        System.out.println("MESSAGE FOUND: " + message.getId());

        Feedback feedback =
                feedbackRepository
                        .findByMessage_Id(messageId)
                        .orElse(
                                Feedback.builder()
                                        .message(message)
                                        .build()
                        );

        feedback.setType(
                type == FeedbackRequest.FeedbackType.LIKE
                        ? Feedback.FeedbackType.LIKE
                        : Feedback.FeedbackType.DISLIKE
        );

        System.out.println("SAVING FEEDBACK: " + feedback.getType());

        feedbackRepository.save(feedback);

        System.out.println("FEEDBACK SAVED");
    }
}
