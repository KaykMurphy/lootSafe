package com.lootsafe.controller;

import com.lootsafe.dto.response.OfferResponseDTO;
import com.lootsafe.enums.MediationDecision;
import com.lootsafe.service.MediationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mediation/offers")
public class MediationController {

    private final MediationService mediationService;

    @GetMapping
    public ResponseEntity<List<OfferResponseDTO>> listMediatedOffers() {

        List<OfferResponseDTO> offers = mediationService.listOffersInMediation();
        return ResponseEntity.ok(offers);
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<OfferResponseDTO> resolveDispute(
            @PathVariable("id") UUID id,
            @RequestParam(name = "decision") MediationDecision decision) throws Exception {

        OfferResponseDTO resolvedOffer = mediationService.resolveDispute(id, decision);
        return ResponseEntity.ok(resolvedOffer);
    }


    @GetMapping("/all")
    public ResponseEntity<Page<OfferResponseDTO>> listAllOffers(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestParam(name = "orderBy", defaultValue = "createdAt") String orderBy,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.valueOf(direction), orderBy);
        Page<OfferResponseDTO> result = mediationService.listAllOffers(pageRequest);

        return ResponseEntity.ok(result);
    }


    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<OfferResponseDTO> cancelManually(
            @PathVariable("id") UUID id,
            @RequestParam(name = "forceRefund") boolean forceRefund) throws Exception {

        OfferResponseDTO result = mediationService.cancelManually(id, forceRefund);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/profit")
    public ResponseEntity<BigDecimal> getPlatformProfit() {
        BigDecimal result = mediationService.calculatePlatformProfit();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/drop")
    public ResponseEntity<OfferResponseDTO> dropMediation(@PathVariable("id") UUID id) {
        OfferResponseDTO response = mediationService.dropMediation(id);
        return ResponseEntity.ok(response);
    }

    // TODO: Remove before production. Test-only endpoint.
    @PostMapping("/{id}/simulate-payment")
    public ResponseEntity<OfferResponseDTO> simulateApprovedPayment(@PathVariable("id") UUID id) {
        OfferResponseDTO result = mediationService.simulateApprovedPayment(id);
        return ResponseEntity.ok(result);
    }

}
