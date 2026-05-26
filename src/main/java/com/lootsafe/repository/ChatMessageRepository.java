package com.lootsafe.repository;

import com.lootsafe.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findAllByOrderBySentAtDesc();

    @Query("select message from ChatMessage message where message.offer.id = :offerId order by message.sentAt asc")
    List<ChatMessage> findMessageHistory(@Param("offerId") UUID offerId);
}
