package com.lootsafe.service;

import com.lootsafe.dto.request.MessageRequestDTO;
import com.lootsafe.dto.response.MessageResponseDTO;
import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.exception.ResourceNotFoundException;
import com.lootsafe.mapper.ChatMessageMapper;
import com.lootsafe.model.ChatMessage;
import com.lootsafe.model.Offer;
import com.lootsafe.repository.ChatMessageRepository;
import com.lootsafe.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setOffer(offer);
        chatMessage.setContent(dto.messageText());
        chatMessage.setMessageAuthor(dto.messageAuthor());

        chatMessageRepository.save(chatMessage);

        return chatMessageMapper.toResponse(chatMessage);
    }

    public List<MessageResponseDTO> getMessageHistory(UUID offerId) {
        List<ChatMessage> messages = chatMessageRepository.findByOfferIdOrderBySentAtAsc(offerId);
        return chatMessageMapper.toResponseList(messages);
    }
}
