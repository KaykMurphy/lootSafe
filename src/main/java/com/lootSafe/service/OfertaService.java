package com.lootSafe.service;

import com.lootSafe.dto.request.OfertaAtualizarDTO;
import com.lootSafe.dto.request.OfertaRequestDTO;
import com.lootSafe.dto.response.OfertaResponseDTO;
import com.lootSafe.enums.StatusTransacao;
import com.lootSafe.exception.ResourceNotFoundException;
import com.lootSafe.mapper.OfertaMapper;
import com.lootSafe.model.EmailDetails;
import com.lootSafe.model.Oferta;
import com.lootSafe.repository.EmailService;
import com.lootSafe.repository.OfertaRepository;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.resources.payment.Payment;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class OfertaService {

    private final OfertaRepository ofertaRepository;
    private final OfertaMapper ofertaMapper;
    private final PagamentoService pagamentoService;
    private final EmailService emailService;

    @Value("${lootsafe.taxa-padrao:0.10}")
    private BigDecimal taxaPadrao;

    public OfertaResponseDTO criarOferta(OfertaRequestDTO request) {

        Oferta oferta = ofertaMapper.toEntity(request);

        BigDecimal taxa = request.valorBruto().multiply(taxaPadrao);
        BigDecimal valorLiquido = request.valorBruto().subtract(taxa);

        oferta.setTaxaPlataforma(taxa);
        oferta.setValorLiquido(valorLiquido);
        oferta.setStatusTransacao(StatusTransacao.AGUARDANDO_PAGAMENTO);

        try {
            Payment pagamentoMP = pagamentoService.gerarPix(request);

            oferta.setMercadoPagoId(pagamentoMP.getId());
            oferta.setCopiaEColaPix(pagamentoMP.getPointOfInteraction().getTransactionData().getQrCode());
            oferta.setQrCodePix(pagamentoMP.getPointOfInteraction().getTransactionData().getQrCodeBase64());

        } catch (MPApiException e) {
            String erroExato = e.getApiResponse().getContent();
            log.error("ERRO DETALHADO DO MERCADO PAGO: {}", erroExato);
            throw new RuntimeException("Mercado Pago recusou o pagamento: " + erroExato);

        } catch (Exception e) {
            throw new RuntimeException("Erro interno ao gerar PIX: " + e.getMessage());
        }

        Oferta ofertaSalva = ofertaRepository.save(oferta);
        return ofertaMapper.toResponseDTO(ofertaSalva);
    }

    public OfertaResponseDTO buscarPorId(UUID id) {
        Oferta response = ofertaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta não encontrada"));
        return ofertaMapper.toResponseDTO(response);
    }

    public Page<OfertaResponseDTO> listarTodas(Pageable pageable) {
        return ofertaRepository.findAll(pageable)
                .map(ofertaMapper::toResponseDTO);
    }

    public void deletarOferta(UUID id) {
        Oferta oferta = ofertaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta não encontrada"));

        List<StatusTransacao> statusBloqueados = List.of(
                StatusTransacao.PAGO_RETIDO,
                StatusTransacao.CONCLUIDO,
                StatusTransacao.EM_MEDIACAO
        );

        if (statusBloqueados.contains(oferta.getStatusTransacao())) {
            throw new IllegalStateException(
                    "Não é possível deletar uma oferta com status: " + oferta.getStatusTransacao()
                            + ". Cancele-a antes de remover."
            );
        }

        ofertaRepository.delete(oferta);
    }

    public OfertaResponseDTO atualizarOferta(UUID id, OfertaAtualizarDTO atualizarDTO) {
        Oferta oferta = ofertaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta não encontrada"));

        ofertaMapper.atualizarOfertaDeDTO(atualizarDTO, oferta);

        if (atualizarDTO.valorBruto() != null) {
            BigDecimal taxa = atualizarDTO.valorBruto().multiply(taxaPadrao);
            BigDecimal valorLiquido = atualizarDTO.valorBruto().subtract(taxa);
            oferta.setTaxaPlataforma(taxa);
            oferta.setValorLiquido(valorLiquido);
        }

        Oferta ofertaAtualizada = ofertaRepository.save(oferta);
        return ofertaMapper.toResponseDTO(ofertaAtualizada);
    }

    @Transactional
    public void processarNotificacaoPagamento(Long mercadoPagoId) {
        try {
            Payment pagamento = pagamentoService.consultarPagamento(mercadoPagoId);

            if ("approved".equals(pagamento.getStatus())) {
                Oferta oferta = ofertaRepository.findByMercadoPagoId(mercadoPagoId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Oferta não encontrada para o pagamento MP: " + mercadoPagoId));

                if (oferta.getStatusTransacao() == StatusTransacao.AGUARDANDO_PAGAMENTO) {
                    oferta.setDataLimiteLiberacao(LocalDateTime.now().plusHours(oferta.getPrazoTesteHoras()));

                    oferta.setStatusTransacao(StatusTransacao.PAGO_RETIDO);
                    ofertaRepository.save(oferta);

                    emailService.enviarMailSimples(getEmailDetails(oferta));

                    oferta.setStatusTransacao(StatusTransacao.CONCLUIDO);
                    ofertaRepository.save(oferta);

                    log.info("Oferta {} concluída. Produto liberado e e-mail enviado.", oferta.getId());
                }
            } else {
                log.info("Pagamento {} status: {}. Nenhuma ação.", mercadoPagoId, pagamento.getStatus());
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar notificação de pagamento: " + e.getMessage(), e);
        }
    }

    public OfertaResponseDTO abrirMediacao(UUID idOferta) {
        Oferta oferta = ofertaRepository.findById(idOferta)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta não encontrada"));

        if (oferta.getStatusTransacao() != StatusTransacao.CONCLUIDO) {
            throw new RuntimeException("A oferta não está em periodo de teste");
        }

        if (LocalDateTime.now().isAfter(oferta.getDataLimiteLiberacao())) {
            throw new RuntimeException("O prazo de garantia já expirou.");
        }

        oferta.setStatusTransacao(StatusTransacao.EM_MEDIACAO);

        Oferta ofertaSalva = ofertaRepository.save(oferta);
        return ofertaMapper.toResponseDTO(ofertaSalva);
    }

    private static @NonNull EmailDetails getEmailDetails(Oferta oferta) {
        String textoEmail = """
                Olá! Seu pagamento foi aprovado com sucesso.
                Sua conta da categoria %s está pronta!
                
                Aqui estão seus dados de acesso:
                Login: %s
                Senha: %s
                
                IMPORTANTE: Você tem até o dia %s para testar a conta.
                Caso tenha algum problema, acesse a sala do pedido para abrir uma mediação:
                https://lootsafe.com.br/pedido/%s
                """.formatted(
                oferta.getCategoriaProduto(),
                oferta.getLoginCredencial(),
                oferta.getSenhaCredencial(),
                oferta.getDataLimiteLiberacao(),
                oferta.getId()
        );

        return new EmailDetails(
                oferta.getEmailComprador(),
                "LootSafe - Sua conta foi liberada!",
                textoEmail
        );
    }
}