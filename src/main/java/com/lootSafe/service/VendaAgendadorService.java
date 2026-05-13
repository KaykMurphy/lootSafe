package com.lootSafe.service;

import com.lootSafe.enums.StatusTransacao;
import com.lootSafe.model.Oferta;
import com.lootSafe.repository.OfertaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VendaAgendadorService {

    private final OfertaRepository ofertaRepository;
    private final PagamentoService pagamentoService;

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cancelarPixExpirados() {
        log.info("Iniciando varredura de PIX expirados (mais de 24h)...");

        LocalDateTime ontem = LocalDateTime.now().minusHours(24);

        List<Oferta> pixNaoPagos = ofertaRepository.findAllByStatusTransacaoAndDataCriacaoBefore(
                StatusTransacao.AGUARDANDO_PAGAMENTO,
                ontem
        );

        for (Oferta oferta : pixNaoPagos) {

            if (oferta.getMercadoPagoId() != null) {
                pagamentoService.cancelarPix(oferta.getMercadoPagoId());
            }

            oferta.setStatusTransacao(StatusTransacao.CANCELADO);
            ofertaRepository.save(oferta);

            log.info("Oferta {} cancelada automaticamente pois o PIX expirou.", oferta.getId());
        }
    }
}