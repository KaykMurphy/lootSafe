package com.lootSafe.controller;

import com.lootSafe.dto.request.MensagemRequestDTO;
import com.lootSafe.dto.request.OfertaAtualizarDTO;
import com.lootSafe.dto.request.OfertaRequestDTO;
import com.lootSafe.dto.response.MensagemResponseDTO;
import com.lootSafe.dto.response.OfertaResponseDTO;
import com.lootSafe.service.ChatService;
import com.lootSafe.service.OfertaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ofertas")
public class OfertaController {

    private final OfertaService ofertaService;
    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<OfertaResponseDTO> criarNovaOferta(@RequestBody @Valid OfertaRequestDTO requestDTO){
        OfertaResponseDTO response = ofertaService.criarOferta(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfertaResponseDTO> buscarOfertaPorId(@PathVariable UUID id){
        OfertaResponseDTO response = ofertaService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<OfertaResponseDTO>> listarTodas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        Page<OfertaResponseDTO> ofertasPaginadas = ofertaService.listarTodas(pageable);

        return ResponseEntity.ok(ofertasPaginadas);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarOferta(@PathVariable UUID id) {

        ofertaService.deletarOferta(id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<OfertaResponseDTO> atualizarProduto(@PathVariable UUID id,
                                                              @RequestBody @Valid OfertaAtualizarDTO atualizarDTO){
        OfertaResponseDTO produtoAtualizado = ofertaService.atualizarOferta(id, atualizarDTO);
        return ResponseEntity.ok(produtoAtualizado);

    }

    @PostMapping("/{id}/mediacao")
    public ResponseEntity<OfertaResponseDTO> abrirMediacao(@PathVariable UUID id){ 
        OfertaResponseDTO produtoEmMediacao = ofertaService.abrirMediacao(id);
        return ResponseEntity.ok(produtoEmMediacao);
    }

    @PostMapping("/{id}/mensagens")
    public ResponseEntity<MensagemResponseDTO> enviarMensagem(
            @PathVariable UUID id,
            @RequestBody MensagemRequestDTO request) {
        MensagemResponseDTO mensagemEnviada = chatService.enviarMensagem(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mensagemEnviada);
    }

    @GetMapping("/{id}/mensagens")
    public ResponseEntity<List<MensagemResponseDTO>> buscarHistorico(@PathVariable UUID id) {
        List<MensagemResponseDTO> historico = chatService.buscarHistorico(id);
        return ResponseEntity.ok(historico);
    }


}
