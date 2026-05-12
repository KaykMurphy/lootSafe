package com.lootSafe.mapper;
import com.lootSafe.dto.request.OfertaAtualizarDTO;
import com.lootSafe.dto.request.OfertaRequestDTO;
import com.lootSafe.dto.response.OfertaResponseDTO;
import com.lootSafe.model.Oferta;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OfertaMapper {

    Oferta toEntity(OfertaRequestDTO dto);

    OfertaResponseDTO toResponseDTO(Oferta entity);

    List<OfertaResponseDTO> listaOfertasResponse(List<Oferta> entityList);

    void atualizarOfertaDeDTO(OfertaAtualizarDTO dto, @MappingTarget Oferta entity);
}