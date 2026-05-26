package com.lootsafe.mapper;
import com.lootsafe.dto.request.OfferUpdateDTO;
import com.lootsafe.dto.request.OfferRequestDTO;
import com.lootsafe.dto.response.OfferResponseDTO;
import com.lootsafe.dto.response.OfferSummaryResponseDTO;
import com.lootsafe.model.Offer;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OfferMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pixQrCode", ignore = true)
    @Mapping(target = "pixCopyPaste", ignore = true)
    @Mapping(target = "mercadoPagoPaymentId", ignore = true)
    @Mapping(target = "platformFee", ignore = true)
    @Mapping(target = "netAmount", ignore = true)
    @Mapping(target = "transactionStatus", ignore = true)
    @Mapping(target = "buyerEmail", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "linkExpiresAt", ignore = true)
    @Mapping(target = "releaseDeadline", ignore = true)
    Offer toEntity(OfferRequestDTO dto);

    OfferResponseDTO toResponseDTO(Offer entity);

    OfferSummaryResponseDTO toSummaryResponseDTO(Offer entity);

    List<OfferResponseDTO> toResponseList(List<Offer> entityList);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pixQrCode", ignore = true)
    @Mapping(target = "pixCopyPaste", ignore = true)
    @Mapping(target = "mercadoPagoPaymentId", ignore = true)
    @Mapping(target = "platformFee", ignore = true)
    @Mapping(target = "netAmount", ignore = true)
    @Mapping(target = "transactionStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "linkExpiresAt", ignore = true)
    @Mapping(target = "releaseDeadline", ignore = true)
    void updateEntityFromDto(OfferUpdateDTO dto, @MappingTarget Offer entity);
}
