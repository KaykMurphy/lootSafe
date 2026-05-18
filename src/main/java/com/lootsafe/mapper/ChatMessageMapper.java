package com.lootsafe.mapper;

import com.lootsafe.dto.request.MessageRequestDTO;
import com.lootsafe.dto.response.MessageResponseDTO;
import com.lootsafe.model.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    @Mapping(source = "messageText", target = "content")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "offer", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    ChatMessage toEntity(MessageRequestDTO dto);

    @Mapping(source = "content", target = "messageText")
    @Mapping(source = "messageAuthor", target = "author")
    MessageResponseDTO toResponse(ChatMessage entity);

    List<MessageResponseDTO> toResponseList(List<ChatMessage> chatMessages);
}
