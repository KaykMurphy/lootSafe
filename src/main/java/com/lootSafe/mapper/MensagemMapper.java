package com.lootSafe.mapper;

import com.lootSafe.dto.request.MensagemRequestDTO;
import com.lootSafe.dto.response.MensagemResponseDTO;
import com.lootSafe.model.MensagemChat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MensagemMapper {

    @Mapping(source = "textoMensagem", target = "texto")
    MensagemChat toEntity(MensagemRequestDTO dto);

    @Mapping(source = "texto", target = "textoMensagem")
    @Mapping(source = "autorMensagem", target = "autor")
    MensagemResponseDTO toResponse(MensagemChat entity);

    List<MensagemResponseDTO> toResponseList(List<MensagemChat> mensagemChats);
}