package com.lootSafe.mapper;

import com.lootSafe.dto.request.MensagemRequestDTO;
import com.lootSafe.dto.response.MensagemResponseDTO;
import com.lootSafe.model.MensagemChat;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MensagemMapper {

    MensagemChat toEntity(MensagemRequestDTO dto);

    MensagemResponseDTO toResponse(MensagemChat entity);

    List<MensagemResponseDTO> toResponseList(List<MensagemChat> mensagemChats);
}
