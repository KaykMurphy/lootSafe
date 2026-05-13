package com.lootSafe.service;

import com.lootSafe.dto.response.OfertaResponseDTO;
import com.lootSafe.enums.DecisaoMediacao;
import com.lootSafe.enums.StatusTransacao;
import com.lootSafe.exception.ResourceNotFoundException;
import com.lootSafe.mapper.OfertaMapper;
import com.lootSafe.model.Oferta;
import com.lootSafe.repository.OfertaRepository;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModeracaoService {

    private final OfertaRepository ofertaRepository;
    private final PagamentoService pagamentoService;
    private final OfertaMapper ofertaMapper;

    public List<OfertaResponseDTO> listarOfertasEmMediacao() {
        List<Oferta> listaOfertas = ofertaRepository.findAllByStatusTransacao(StatusTransacao.EM_MEDIACAO);
        return ofertaMapper.listaOfertasResponse(listaOfertas);
    }

    public OfertaResponseDTO resolverConflito(UUID idOferta, DecisaoMediacao decisao) throws MPException, MPApiException {
        Oferta oferta = ofertaRepository.findById(idOferta)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta não encontrada"));

        if (oferta.getStatusTransacao() != StatusTransacao.EM_MEDIACAO) {
            throw new IllegalStateException("Você não pode julgar uma oferta que não está em disputa.");
        }

        if (DecisaoMediacao.FAVOR_COMPRADOR.equals(decisao)) {
            pagamentoService.reembolsoPagamento(oferta.getMercadoPagoId());
            oferta.setStatusTransacao(StatusTransacao.REEMBOLSADO);
            log.info("Mediação da oferta {} resolvida a favor do comprador. Reembolso iniciado.", idOferta);
        }

        if (DecisaoMediacao.FAVOR_VENDEDOR.equals(decisao)) {

            try {
                pagamentoService.enviarParaVendedor(
                        oferta.getChavePix(),
                        oferta.getTipoChavePix(),
                        oferta.getValorLiquido(),
                        oferta.getId()
                );
                oferta.setStatusTransacao(StatusTransacao.FINALIZADO);
                log.info("Mediação da oferta {} resolvida a favor do vendedor. Repasse de {} iniciado.",
                        idOferta, oferta.getValorLiquido());

            } catch (Exception e) {

                log.error("Falha ao enviar repasse para o vendedor da oferta {}: {}", idOferta, e.getMessage());
                throw new RuntimeException(
                        "Decisão registrada, mas o repasse para o vendedor falhou: " + e.getMessage(), e
                );
            }
        }

        Oferta mediacaoResolvida = ofertaRepository.save(oferta);
        return ofertaMapper.toResponseDTO(mediacaoResolvida);
    }

    public Page<OfertaResponseDTO> listarTodasAsOfertas(Pageable paginacao) {
        return ofertaRepository.findAll(paginacao)
                .map(ofertaMapper::toResponseDTO);
    }

    public OfertaResponseDTO cancelarManual(UUID idOferta, boolean forcarReembolso) throws MPException, MPApiException {
        Oferta oferta = ofertaRepository.findById(idOferta)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta não encontrada"));

        if (oferta.getStatusTransacao() != StatusTransacao.PAGO_RETIDO
                && oferta.getStatusTransacao() != StatusTransacao.EM_MEDIACAO) {
            throw new IllegalStateException("A oferta não pode ser cancelada neste status");
        }

        if (forcarReembolso) {
            pagamentoService.reembolsoPagamento(oferta.getMercadoPagoId());
        }

        oferta.setStatusTransacao(StatusTransacao.CANCELADO);
        Oferta ofertaSalva = ofertaRepository.save(oferta);

        return ofertaMapper.toResponseDTO(ofertaSalva);
    }

    public BigDecimal calcularLucroPlataforma() {

        BigDecimal lucro = ofertaRepository.calcularLucroTotalPorStatuses(
                List.of(StatusTransacao.FINALIZADO, StatusTransacao.CONCLUIDO)
        );

        return lucro != null ? lucro : BigDecimal.ZERO;
    }
}