package com.lootsafe.service;

import com.lootsafe.dto.request.MessageRequestDTO;
import com.lootsafe.dto.response.MessageResponseDTO;
import com.lootsafe.enums.MessageType;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.mapper.ChatMessageMapper;
import com.lootsafe.model.ChatMessage;
import com.lootsafe.model.Offer;
import com.lootsafe.repository.ChatMessageRepository;
import com.lootsafe.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final OfferRepository offerRepository;
    private final ChatMessageMapper chatMessageMapper;

    public MessageResponseDTO sendMessage(UUID offerId, MessageRequestDTO dto) {

        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (offer.getTransactionStatus() != TransactionStatus.IN_MEDIATION) {
            throw new RuntimeException("Messages are only allowed during active mediation.");
        }

        ChatMessage chatMessage = chatMessageMapper.toEntity(normalizeMessage(dto));
        chatMessage.setOffer(offer);

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        return chatMessageMapper.toResponse(savedMessage);
    }

    @Transactional
    public MessageResponseDTO savePublicMessage(MessageRequestDTO input) {
        ChatMessage message = chatMessageMapper.toEntity(normalizeMessage(input));
        ChatMessage savedMessage = chatMessageRepository.save(message);
        return chatMessageMapper.toResponse(savedMessage);
    }

    public List<MessageResponseDTO> getLatestMessages(int quantity) {
        List<ChatMessage> messages = chatMessageRepository.findAllByOrderBySentAtDesc();
        return messages.stream()
                .limit(quantity)
                .sorted((m1, m2) -> m1.getSentAt().compareTo(m2.getSentAt()))
                .map(chatMessageMapper::toResponse)
                .toList();
    }

    public List<MessageResponseDTO> getMessageHistory(UUID offerId) {
        return chatMessageMapper.toResponseList(chatMessageRepository.findMessageHistory(offerId));
    }

    private MessageRequestDTO normalizeMessage(MessageRequestDTO input) {
        MessageType messageType = input.messageType() == null ? MessageType.CHAT : input.messageType();
        return new MessageRequestDTO(input.author(), input.messageText(), input.sentAt(), messageType);
    }
}
