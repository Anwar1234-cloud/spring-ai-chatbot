package com.springai.chatbot.repository;

import com.springai.chatbot.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedbackRepository
        extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByMessage_Id(Long messageId);
}
