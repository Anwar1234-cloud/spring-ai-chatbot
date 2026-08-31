package com.springai.chatbot.service;

import com.springai.chatbot.dto.FeedbackRequest;
import com.springai.chatbot.entity.ChatMessage;
import com.springai.chatbot.entity.Conversation;
import com.springai.chatbot.entity.Feedback;
import com.springai.chatbot.repository.ChatMessageRepository;
import com.springai.chatbot.repository.ConversationRepository;
import com.springai.chatbot.repository.FeedbackRepository;

import org.apache.pdfbox.Loader;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final FeedbackRepository feedbackRepository;
    private final PdfService pdfService;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository,
            FeedbackRepository feedbackRepository,
            PdfService pdfService
    ) {
        this.chatClient = chatClientBuilder.build();
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.feedbackRepository = feedbackRepository;
        this.pdfService = pdfService;
    }


    // ============================================================
    // NORMAL TEXT CHAT
    // ============================================================

    @Transactional
    public ChatResult chat(
            String message,
            String conversationId
    ) {

        Conversation conversation;

        // --------------------------------------------------------
        // 1. FIND OR CREATE CONVERSATION
        // --------------------------------------------------------

        if (conversationId == null || conversationId.isBlank()) {

            String title =
                    message == null || message.isBlank()
                            ? "New Conversation"
                            : message.trim();

            if (title.length() > 50) {
                title = title.substring(0, 50) + "...";
            }

            conversation =
                    conversationRepository.save(
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

            conversation =
                    conversationRepository
                            .findById(id)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Conversation not found: "
                                                    + conversationId
                                    )
                            );
        }


        // --------------------------------------------------------
        // 2. LOAD PREVIOUS MESSAGES
        // --------------------------------------------------------

        List<ChatMessage> previousMessages =
                chatMessageRepository
                        .findByConversation_IdOrderByCreatedAtAsc(
                                conversation.getId()
                        );


        // --------------------------------------------------------
        // 3. BUILD AI HISTORY
        // --------------------------------------------------------

        List<org.springframework.ai.chat.messages.Message>
                aiMessages = new ArrayList<>();

        for (ChatMessage chatMessage : previousMessages) {

            if (chatMessage.getRole()
                    == ChatMessage.Role.USER) {

                aiMessages.add(
                        new UserMessage(
                                chatMessage.getContent()
                        )
                );

            } else {

                aiMessages.add(
                        new AssistantMessage(
                                chatMessage.getContent()
                        )
                );
            }
        }


        // --------------------------------------------------------
        // 4. CALL AI
        // --------------------------------------------------------

        String aiResponse;

        if (aiMessages.isEmpty()) {

            aiResponse =
                    chatClient
                            .prompt()
                            .user(message)
                            .call()
                            .content();

        } else {

            aiResponse =
                    chatClient
                            .prompt()
                            .messages(aiMessages)
                            .user(message)
                            .call()
                            .content();
        }


        // --------------------------------------------------------
        // 5. SAVE USER MESSAGE
        // --------------------------------------------------------

        ChatMessage userMessage =
                ChatMessage.builder()
                        .conversation(conversation)
                        .role(ChatMessage.Role.USER)
                        .content(message)
                        .build();

        chatMessageRepository.save(userMessage);


        // --------------------------------------------------------
        // 6. SAVE ASSISTANT MESSAGE
        // --------------------------------------------------------

        ChatMessage assistantMessage =
                ChatMessage.builder()
                        .conversation(conversation)
                        .role(ChatMessage.Role.ASSISTANT)
                        .content(aiResponse)
                        .build();

        assistantMessage =
                chatMessageRepository.save(assistantMessage);


        // --------------------------------------------------------
        // 7. RETURN INCLUDING MESSAGE ID
        // --------------------------------------------------------

        return new ChatResult(
                conversation.getId().toString(),
                aiResponse,
                assistantMessage.getId()
        );
    }


    // ============================================================
    // STREAMING TEXT CHAT
    // ============================================================

    public Flux<String> streamChat(
            String message,
            String conversationId
    ) {

        Conversation conversation;


        // --------------------------------------------------------
        // 1. FIND OR CREATE CONVERSATION
        // --------------------------------------------------------

        if (conversationId == null || conversationId.isBlank()) {

            String title =
                    message == null || message.isBlank()
                            ? "New Conversation"
                            : message.trim();

            if (title.length() > 50) {
                title = title.substring(0, 50) + "...";
            }

            conversation =
                    conversationRepository.save(
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
                        "Invalid conversationId: "
                                + conversationId
                );
            }

            conversation =
                    conversationRepository
                            .findById(id)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Conversation not found: "
                                                    + conversationId
                                    )
                            );
        }


        // --------------------------------------------------------
        // 2. LOAD PREVIOUS MESSAGES
        // --------------------------------------------------------

        List<ChatMessage> previousMessages =
                chatMessageRepository
                        .findByConversation_IdOrderByCreatedAtAsc(
                                conversation.getId()
                        );


        // --------------------------------------------------------
        // 3. BUILD AI HISTORY
        // --------------------------------------------------------

        List<org.springframework.ai.chat.messages.Message>
                aiMessages = new ArrayList<>();

        for (ChatMessage chatMessage :
                previousMessages) {

            if (chatMessage.getRole()
                    == ChatMessage.Role.USER) {

                aiMessages.add(
                        new UserMessage(
                                chatMessage.getContent()
                        )
                );

            } else {

                aiMessages.add(
                        new AssistantMessage(
                                chatMessage.getContent()
                        )
                );
            }
        }


        // --------------------------------------------------------
        // 4. STREAM RESPONSE
        // --------------------------------------------------------

        Flux<String> responseFlux;

        if (aiMessages.isEmpty()) {

            responseFlux =
                    chatClient
                            .prompt()
                            .user(message)
                            .stream()
                            .content();

        } else {

            responseFlux =
                    chatClient
                            .prompt()
                            .messages(aiMessages)
                            .user(message)
                            .stream()
                            .content();
        }


        // --------------------------------------------------------
        // 5. COLLECT COMPLETE RESPONSE
        // --------------------------------------------------------

        StringBuilder fullResponse =
                new StringBuilder();

        return responseFlux

                .doOnNext(fullResponse::append)

                .doOnComplete(() -> {

                    // Save USER message

                    ChatMessage userMessage =
                            ChatMessage.builder()
                                    .conversation(conversation)
                                    .role(ChatMessage.Role.USER)
                                    .content(message)
                                    .build();

                    chatMessageRepository.save(userMessage);


                    // Save ASSISTANT message

                    ChatMessage assistantMessage =
                            ChatMessage.builder()
                                    .conversation(conversation)
                                    .role(ChatMessage.Role.ASSISTANT)
                                    .content(
                                            fullResponse.toString()
                                    )
                                    .build();

                    assistantMessage =
                            chatMessageRepository.save(
                                    assistantMessage
                            );

                    System.out.println(
                            "STREAM RESPONSE SAVED"
                    );

                    System.out.println(
                            "ASSISTANT MESSAGE ID: "
                                    + assistantMessage.getId()
                    );
                });
    }


    // ============================================================
    // STREAMING CHAT WITH FILE
    // ============================================================

    public Flux<String> streamChat(
            String message,
            String conversationId,
            MultipartFile file
    ) {

        Conversation conversation;


        // --------------------------------------------------------
        // 1. FIND OR CREATE CONVERSATION
        // --------------------------------------------------------

        if (conversationId == null || conversationId.isBlank()) {

            String title;

            if (file != null && !file.isEmpty()) {
                title = file.getOriginalFilename();
            } else {
                title = message;
            }

            if (title == null || title.isBlank()) {
                title = "New Conversation";
            }

            if (title.length() > 50) {
                title = title.substring(0, 50) + "...";
            }

            conversation =
                    conversationRepository.save(
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
                        "Invalid conversationId: "
                                + conversationId
                );
            }

            conversation =
                    conversationRepository
                            .findById(id)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Conversation not found: "
                                                    + conversationId
                                    )
                            );
        }


        // --------------------------------------------------------
        // 2. LOAD PREVIOUS MESSAGES
        // --------------------------------------------------------

        List<ChatMessage> previousMessages =
                chatMessageRepository
                        .findByConversation_IdOrderByCreatedAtAsc(
                                conversation.getId()
                        );


        List<org.springframework.ai.chat.messages.Message>
                aiMessages = new ArrayList<>();

        for (ChatMessage chatMessage :
                previousMessages) {

            if (chatMessage.getRole()
                    == ChatMessage.Role.USER) {

                aiMessages.add(
                        new UserMessage(
                                chatMessage.getContent()
                        )
                );

            } else {

                aiMessages.add(
                        new AssistantMessage(
                                chatMessage.getContent()
                        )
                );
            }
        }


        // --------------------------------------------------------
        // 3. PROCESS FILE
        // --------------------------------------------------------

        String finalMessage = message;

        if (file != null && !file.isEmpty()) {

            String filename =
                    file.getOriginalFilename();

            if (filename == null) {
                filename = "uploaded-file";
            }

            System.out.println(
                    "STREAM FILE RECEIVED: "
                            + filename
            );

            System.out.println(
                    "STREAM FILE TYPE: "
                            + file.getContentType()
            );

            System.out.println(
                    "STREAM FILE SIZE: "
                            + file.getSize()
            );


            // ----------------------------------------------------
            // PDF
            // ----------------------------------------------------

            if ("application/pdf".equals(
                    file.getContentType()
            )) {

                try {

                    org.apache.pdfbox.pdmodel.PDDocument
                            document =
                            Loader.loadPDF(
                                    file.getBytes()
                            );

                    org.apache.pdfbox.text.PDFTextStripper
                            stripper =
                            new org.apache.pdfbox.text.PDFTextStripper();

                    String pdfText =
                            stripper.getText(document);

                    document.close();

                    finalMessage =
                            (
                                    message == null
                                            || message.isBlank()
                                            ? "Analyze this PDF."
                                            : message
                            )
                                    +
                                    "\n\nPDF FILE: "
                                    +
                                    filename
                                    +
                                    "\n\nPDF CONTENT:\n"
                                    +
                                    pdfText;

                } catch (Exception e) {

                    e.printStackTrace();

                    throw new RuntimeException(
                            "Unable to read PDF file."
                    );
                }
            }


            // ----------------------------------------------------
            // IMAGE
            // ----------------------------------------------------

            else if (
                    file.getContentType() != null
                            &&
                            file.getContentType()
                                    .startsWith("image/")
            ) {

                finalMessage =
                        (
                                message == null
                                        || message.isBlank()
                                        ? "Analyze this image."
                                        : message
                        )
                                +
                                "\n\nAttached image: "
                                +
                                filename;
            }


            // ----------------------------------------------------
            // UNSUPPORTED
            // ----------------------------------------------------

            else {

                throw new IllegalArgumentException(
                        "Only PDF and image files are supported."
                );
            }
        }


        // --------------------------------------------------------
        // 4. STREAM AI RESPONSE
        // --------------------------------------------------------

        Flux<String> responseFlux;

        if (aiMessages.isEmpty()) {

            responseFlux =
                    chatClient
                            .prompt()
                            .user(finalMessage)
                            .stream()
                            .content();

        } else {

            responseFlux =
                    chatClient
                            .prompt()
                            .messages(aiMessages)
                            .user(finalMessage)
                            .stream()
                            .content();
        }


        // --------------------------------------------------------
        // 5. SAVE AFTER STREAM COMPLETES
        // --------------------------------------------------------

        StringBuilder fullResponse =
                new StringBuilder();

        return responseFlux

                .doOnNext(fullResponse::append)

                .doOnComplete(() -> {

                    String savedUserContent =
                            message;

                    if (savedUserContent == null
                            || savedUserContent.isBlank()) {

                        savedUserContent =
                                file != null
                                        ? "Uploaded: "
                                        + file.getOriginalFilename()
                                        : "";
                    }


                    // Save USER

                    ChatMessage userMessage =
                            ChatMessage.builder()
                                    .conversation(conversation)
                                    .role(ChatMessage.Role.USER)
                                    .content(savedUserContent)
                                    .build();

                    chatMessageRepository.save(
                            userMessage
                    );


                    // Save ASSISTANT

                    ChatMessage assistantMessage =
                            ChatMessage.builder()
                                    .conversation(conversation)
                                    .role(ChatMessage.Role.ASSISTANT)
                                    .content(
                                            fullResponse.toString()
                                    )
                                    .build();

                    assistantMessage =
                            chatMessageRepository.save(
                                    assistantMessage
                            );


                    System.out.println(
                            "STREAM FILE RESPONSE SAVED"
                    );

                    System.out.println(
                            "ASSISTANT MESSAGE ID: "
                                    + assistantMessage.getId()
                    );
                });
    }


    // ============================================================
    // CREATE CONVERSATION TITLE
    // ============================================================

    private String createConversationTitle(
            String message
    ) {

        if (message == null || message.isBlank()) {
            return "New Conversation";
        }

        String title = message.trim();

        if (title.length() > 40) {
            title = title.substring(0, 40) + "...";
        }

        return title;
    }


    // ============================================================
    // CHAT RESULT
    // ============================================================

    public record ChatResult(
            String conversationId,
            String response,
            Long messageId
    ) {
    }


    // ============================================================
    // REGENERATE RESPONSE
    // ============================================================

    @Transactional
    public ChatResult regenerate(
            String conversationId,
            Long assistantMessageId
    ) {

        // --------------------------------------------------------
        // 1. VALIDATE CONVERSATION ID
        // --------------------------------------------------------

        UUID conversationUUID;

        try {

            conversationUUID =
                    UUID.fromString(conversationId);

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "Invalid conversationId: "
                            + conversationId
            );
        }


        // --------------------------------------------------------
        // 2. FIND CONVERSATION
        // --------------------------------------------------------

        Conversation conversation =
                conversationRepository
                        .findById(conversationUUID)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Conversation not found: "
                                                + conversationId
                                )
                        );


        // --------------------------------------------------------
        // 3. FIND ASSISTANT MESSAGE
        // --------------------------------------------------------

        ChatMessage assistantMessage =
                chatMessageRepository
                        .findById(assistantMessageId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Assistant message not found: "
                                                + assistantMessageId
                                )
                        );


        // --------------------------------------------------------
        // 4. VERIFY MESSAGE BELONGS TO CONVERSATION
        // --------------------------------------------------------

        if (!assistantMessage
                .getConversation()
                .getId()
                .equals(conversationUUID)) {

            throw new IllegalArgumentException(
                    "Message does not belong to this conversation"
            );
        }


        // --------------------------------------------------------
        // 5. VERIFY ASSISTANT MESSAGE
        // --------------------------------------------------------

        if (assistantMessage.getRole()
                != ChatMessage.Role.ASSISTANT) {

            throw new IllegalArgumentException(
                    "Only assistant messages can be regenerated"
            );
        }


        // --------------------------------------------------------
        // 6. LOAD ALL MESSAGES
        // --------------------------------------------------------

        List<ChatMessage> allMessages =
                chatMessageRepository
                        .findByConversation_IdOrderByCreatedAtAsc(
                                conversationUUID
                        );


        // --------------------------------------------------------
        // 7. FIND ASSISTANT MESSAGE POSITION
        // --------------------------------------------------------

        int assistantIndex = -1;

        for (int i = 0;
             i < allMessages.size();
             i++) {

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


        // --------------------------------------------------------
        // 8. GET PREVIOUS USER MESSAGE
        // --------------------------------------------------------

        ChatMessage userMessage =
                allMessages.get(
                        assistantIndex - 1
                );


        if (userMessage.getRole()
                != ChatMessage.Role.USER) {

            throw new IllegalArgumentException(
                    "Assistant message has no previous user message"
            );
        }


        // --------------------------------------------------------
        // 9. BUILD HISTORY BEFORE OLD ASSISTANT RESPONSE
        // --------------------------------------------------------

        List<org.springframework.ai.chat.messages.Message>
                aiMessages = new ArrayList<>();

        for (int i = 0;
             i < assistantIndex;
             i++) {

            ChatMessage chatMessage =
                    allMessages.get(i);

            if (chatMessage.getRole()
                    == ChatMessage.Role.USER) {

                aiMessages.add(
                        new UserMessage(
                                chatMessage.getContent()
                        )
                );

            } else {

                aiMessages.add(
                        new AssistantMessage(
                                chatMessage.getContent()
                        )
                );
            }
        }


        // --------------------------------------------------------
        // 10. DELETE OLD ASSISTANT RESPONSE
        // --------------------------------------------------------

        chatMessageRepository.delete(
                assistantMessage
        );


        // --------------------------------------------------------
        // 11. GENERATE NEW RESPONSE
        // --------------------------------------------------------

        String newResponse =
                chatClient
                        .prompt()
                        .messages(aiMessages)
                        .call()
                        .content();


        // --------------------------------------------------------
        // 12. SAVE NEW ASSISTANT MESSAGE
        // --------------------------------------------------------

        ChatMessage newAssistantMessage =
                ChatMessage.builder()
                        .conversation(conversation)
                        .role(ChatMessage.Role.ASSISTANT)
                        .content(newResponse)
                        .build();


        newAssistantMessage =
                chatMessageRepository.save(
                        newAssistantMessage
                );


        System.out.println(
                "REGENERATED MESSAGE SAVED"
        );

        System.out.println(
                "NEW ASSISTANT MESSAGE ID: "
                        + newAssistantMessage.getId()
        );


        // --------------------------------------------------------
        // 13. RETURN NEW MESSAGE ID
        // --------------------------------------------------------

        return new ChatResult(
                conversation.getId().toString(),
                newResponse,
                newAssistantMessage.getId()
        );
    }


    // ============================================================
    // FEEDBACK
    // ============================================================

    @Transactional
    public void saveFeedback(
            Long messageId,
            FeedbackRequest.FeedbackType type
    ) {

        System.out.println(
                "FEEDBACK MESSAGE ID: "
                        + messageId
        );

        System.out.println(
                "FEEDBACK TYPE: "
                        + type
        );


        ChatMessage message =
                chatMessageRepository
                        .findById(messageId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Message not found: "
                                                + messageId
                                )
                        );


        System.out.println(
                "MESSAGE FOUND: "
                        + message.getId()
        );


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


        System.out.println(
                "SAVING FEEDBACK: "
                        + feedback.getType()
        );


        feedbackRepository.save(
                feedback
        );


        System.out.println(
                "FEEDBACK SAVED"
        );
    }


    // ============================================================
    // NORMAL CHAT WITH PDF / IMAGE
    // ============================================================

    @Transactional
    public ChatResult chat(
            String message,
            String conversationId,
            MultipartFile file
    ) {

        Conversation conversation;


        // --------------------------------------------------------
        // 1. FIND OR CREATE CONVERSATION
        // --------------------------------------------------------

        if (conversationId == null || conversationId.isBlank()) {

            String title;

            if (file != null && !file.isEmpty()) {

                title =
                        file.getOriginalFilename();

            } else {

                title =
                        message == null
                                ? null
                                : message.trim();
            }


            if (title == null || title.isBlank()) {
                title = "New Conversation";
            }


            if (title.length() > 50) {
                title =
                        title.substring(0, 50)
                                + "...";
            }


            conversation =
                    conversationRepository.save(
                            Conversation.builder()
                                    .title(title)
                                    .build()
                    );

        } else {

            UUID id;

            try {

                id =
                        UUID.fromString(
                                conversationId
                        );

            } catch (IllegalArgumentException e) {

                throw new IllegalArgumentException(
                        "Invalid conversationId: "
                                + conversationId
                );
            }


            conversation =
                    conversationRepository
                            .findById(id)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Conversation not found: "
                                                    + conversationId
                                    )
                            );
        }


        // --------------------------------------------------------
        // 2. LOAD PREVIOUS MESSAGES
        // --------------------------------------------------------

        List<ChatMessage> previousMessages =
                chatMessageRepository
                        .findByConversation_IdOrderByCreatedAtAsc(
                                conversation.getId()
                        );


        // --------------------------------------------------------
        // 3. BUILD AI HISTORY
        // --------------------------------------------------------

        List<org.springframework.ai.chat.messages.Message>
                aiMessages = new ArrayList<>();


        for (ChatMessage chatMessage :
                previousMessages) {

            if (chatMessage.getRole()
                    == ChatMessage.Role.USER) {

                aiMessages.add(
                        new UserMessage(
                                chatMessage.getContent()
                        )
                );

            } else {

                aiMessages.add(
                        new AssistantMessage(
                                chatMessage.getContent()
                        )
                );
            }
        }


        // --------------------------------------------------------
        // 4. PROCESS FILE
        // --------------------------------------------------------

        String finalMessage = message;


        if (file != null && !file.isEmpty()) {

            String filename =
                    file.getOriginalFilename();


            if (filename == null) {
                filename = "uploaded-file";
            }


            System.out.println(
                    "FILE RECEIVED: "
                            + filename
            );

            System.out.println(
                    "FILE TYPE: "
                            + file.getContentType()
            );

            System.out.println(
                    "FILE SIZE: "
                            + file.getSize()
            );


            // ----------------------------------------------------
            // PDF
            // ----------------------------------------------------

            if ("application/pdf".equals(
                    file.getContentType()
            )) {

                try {

                    org.apache.pdfbox.pdmodel.PDDocument
                            document =
                            Loader.loadPDF(
                                    file.getBytes()
                            );


                    org.apache.pdfbox.text.PDFTextStripper
                            stripper =
                            new org.apache.pdfbox.text.PDFTextStripper();


                    String pdfText =
                            stripper.getText(
                                    document
                            );


                    document.close();


                    finalMessage =
                            (
                                    message == null
                                            || message.isBlank()
                                            ? "Analyze this PDF."
                                            : message
                            )
                                    +
                                    "\n\nPDF FILE: "
                                    +
                                    filename
                                    +
                                    "\n\nPDF CONTENT:\n"
                                    +
                                    pdfText;


                } catch (Exception e) {

                    e.printStackTrace();

                    throw new RuntimeException(
                            "Unable to read PDF file."
                    );
                }
            }


            // ----------------------------------------------------
            // IMAGE
            // ----------------------------------------------------

            else if (
                    file.getContentType() != null
                            &&
                            file.getContentType()
                                    .startsWith("image/")
            ) {

                finalMessage =
                        (
                                message == null
                                        || message.isBlank()
                                        ? "Analyze this image."
                                        : message
                        )
                                +
                                "\n\nAttached image: "
                                +
                                filename;
            }


            // ----------------------------------------------------
            // UNSUPPORTED
            // ----------------------------------------------------

            else {

                throw new IllegalArgumentException(
                        "Only PDF and image files are supported."
                );
            }
        }


        // --------------------------------------------------------
        // 5. CALL AI
        // --------------------------------------------------------

        String aiResponse;


        if (aiMessages.isEmpty()) {

            aiResponse =
                    chatClient
                            .prompt()
                            .user(finalMessage)
                            .call()
                            .content();

        } else {

            aiResponse =
                    chatClient
                            .prompt()
                            .messages(aiMessages)
                            .user(finalMessage)
                            .call()
                            .content();
        }


        // --------------------------------------------------------
        // 6. SAVE USER MESSAGE
        // --------------------------------------------------------

        String savedUserContent =
                message;


        if (savedUserContent == null
                || savedUserContent.isBlank()) {

            savedUserContent =
                    file != null
                            ? "Uploaded: "
                            + file.getOriginalFilename()
                            : "";
        }


        ChatMessage userMessage =
                ChatMessage.builder()
                        .conversation(conversation)
                        .role(ChatMessage.Role.USER)
                        .content(savedUserContent)
                        .build();


        chatMessageRepository.save(
                userMessage
        );


        // --------------------------------------------------------
        // 7. SAVE ASSISTANT MESSAGE
        // --------------------------------------------------------

        ChatMessage assistantMessage =
                ChatMessage.builder()
                        .conversation(conversation)
                        .role(ChatMessage.Role.ASSISTANT)
                        .content(aiResponse)
                        .build();


        assistantMessage =
                chatMessageRepository.save(
                        assistantMessage
                );


        // --------------------------------------------------------
        // 8. RETURN MESSAGE ID
        // --------------------------------------------------------

        return new ChatResult(
                conversation.getId().toString(),
                aiResponse,
                assistantMessage.getId()
        );
    }
}