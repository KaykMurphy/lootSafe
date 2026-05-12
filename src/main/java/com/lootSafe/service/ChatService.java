package com.lootSafe.service;

import com.lootSafe.dto.request.MensagemRequestDTO;
import com.lootSafe.dto.response.MensagemResponseDTO;
import com.lootSafe.enums.StatusTransacao;
import com.lootSafe.exception.ResourceNotFoundException;
import com.lootSafe.mapper.MensagemMapper;
import com.lootSafe.model.MensagemChat;
import com.lootSafe.model.Oferta;
import com.lootSafe.repository.MensagemChatRepository;
import com.lootSafe.repository.OfertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MensagemChatRepository mensagemChatRepository;
    private final OfertaRepository ofertaRepository;
    private final MensagemMapper mensagemMapper;

    public MensagemResponseDTO enviarMensagem(UUID idOferta, MensagemRequestDTO dto){

        Oferta oferta = ofertaRepository.findById(idOferta)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta não encontrada"));

        if (oferta.getStatusTransacao() != StatusTransacao.EM_MEDIACAO){
            throw new RuntimeException("As mensagens só são permitidas durante uma mediação ativa.");
        }

        MensagemChat mensagemChat = new MensagemChat();
        mensagemChat.setOferta(oferta);
        mensagemChat.setTexto(dto.textoMensagem());
        mensagemChat.setAutorMensagem(dto.autorMensagem());

        mensagemChatRepository.save(mensagemChat);

        return mensagemMapper.toResponse(mensagemChat);

    }

    public List<MensagemResponseDTO> buscarHistorico(UUID idOferta) {
        List<MensagemChat> listaMensagens = mensagemChatRepository.findByOfertaIdOrderByDataEnvioAsc(idOferta);
        return mensagemMapper.toResponseList(listaMensagens);
    }
}
