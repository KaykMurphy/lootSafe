package com.lootsafe.controller;

import com.lootsafe.dto.request.MessageRequestDTO;
import com.lootsafe.dto.request.OfferUpdateDTO;
import com.lootsafe.dto.request.OfferRequestDTO;
import com.lootsafe.dto.response.MessageResponseDTO;
import com.lootsafe.dto.response.OfferResponseDTO;
import com.lootsafe.dto.response.OfferSummaryResponseDTO;
import com.lootsafe.service.ChatService;
import com.lootsafe.service.MediationService;
import com.lootsafe.service.OfferService;
import jakarta.validation.constraints.Email;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/offers")
public class OfferController {

    private final OfferService offerService;
    private final ChatService chatService;
    private final MediationService mediationService;

    @PostMapping
    public ResponseEntity<OfferResponseDTO> createOffer(
            @RequestBody @Valid OfferRequestDTO requestDto, Authentication authentication) {

        String loggedUserIdentifier = authentication.getName();


        OfferResponseDTO response = offerService.createOffer(requestDto, loggedUserIdentifier);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/{id}/generate-pix")
    public ResponseEntity<OfferResponseDTO> generatePixForBuyer(
            @PathVariable("id") UUID id,
            @RequestParam(name = "buyerEmail") @NotBlank @Email String buyerEmail,
            @RequestParam(name = "buyerFirstName", required = false) String buyerFirstName,
            @RequestParam(name = "buyerLastName", required = false) String buyerLastName,
            @RequestParam(name = "documentType", defaultValue = "CPF") String documentType,
            @RequestParam(name = "documentNumber", required = false) String documentNumber
    ) {
        OfferResponseDTO response = offerService.generatePixForBuyer(
                id,
                buyerEmail,
                buyerFirstName,
                buyerLastName,
                documentType,
                documentNumber
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfferResponseDTO> getOfferById(@PathVariable("id") UUID id) {

        OfferResponseDTO response = offerService.getById(id);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/release-payment")
    public ResponseEntity<OfferResponseDTO> releasePayment(@PathVariable("id") UUID id,
                                                           Authentication authentication ) {

        String loggedUserIdentifier = authentication.getName();

        OfferResponseDTO releasedOffer = offerService.releasePayment(id, loggedUserIdentifier);

        return ResponseEntity.ok(releasedOffer);
    }

    @GetMapping
    public ResponseEntity<Page<OfferSummaryResponseDTO>> listAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        return ResponseEntity.ok(offerService.listAll(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffer(@PathVariable("id") UUID id, Authentication authentication) {

        String loggedUserIdentifier = authentication.getName();

        offerService.deleteOffer(id, loggedUserIdentifier);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<OfferResponseDTO> updateOffer(@PathVariable("id") UUID id,
                                                        @RequestBody @Valid OfferUpdateDTO updateDto,
                                                        Authentication authentication) {

        String loggedUserIdentifier = authentication.getName();

        OfferResponseDTO updatedOffer = offerService.updateOffer(id, updateDto, loggedUserIdentifier);

        return ResponseEntity.ok(updatedOffer);
    }

    @PostMapping("/{id}/mediation")
    public ResponseEntity<OfferResponseDTO> openMediation(@PathVariable("id") UUID id,
                                                          Authentication authentication) {

        String loggedUserIdentifier = authentication.getName();

        OfferResponseDTO offerInMediation = offerService.openMediation(id, loggedUserIdentifier);

        return ResponseEntity.ok(offerInMediation);
    }

    @PostMapping("/{id}/mediation/drop")
    public ResponseEntity<OfferResponseDTO> dropMediationByBuyer(@PathVariable("id") UUID id, Authentication authentication) {

        String loggedUserIdentifier = authentication.getName();

        OfferResponseDTO response = mediationService.dropMediationByBuyer(id, loggedUserIdentifier);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponseDTO> sendMessage(
            @PathVariable("id") UUID id,
            @RequestBody MessageRequestDTO request,
            Authentication authentication
    ) {

        String loggedUserIdentifier = authentication.getName();

        MessageResponseDTO sentMessage = chatService.sendMessage(id, request, loggedUserIdentifier);

        return ResponseEntity.status(HttpStatus.CREATED).body(sentMessage);
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageResponseDTO>> getMessageHistory(@PathVariable("id") UUID id, Authentication authentication)
    {
        String loggedUserIdentifier = authentication.getName();

        return ResponseEntity.ok(chatService.getMessageHistory(id, loggedUserIdentifier));
    }

    @GetMapping("/my-sales")
    public ResponseEntity<Page<OfferSummaryResponseDTO>> getMySales(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        return ResponseEntity.ok(offerService.listMySales(authentication.getName(), pageable));
    }

    @GetMapping("/my-purchases")
    public ResponseEntity<Page<OfferSummaryResponseDTO>> getMyPurchases(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        return ResponseEntity.ok(offerService.listMyPurchases(authentication.getName(), pageable));
    }
}
