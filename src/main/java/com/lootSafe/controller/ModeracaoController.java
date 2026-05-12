package com.lootSafe.controller;

import com.lootSafe.dto.response.OfertaResponseDTO;
import com.lootSafe.enums.DecisaoMediacao;
import com.lootSafe.service.ModeracaoService;
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
@RequestMapping("/api/moderacao/ofertas")
public class ModeracaoController {

    private final ModeracaoService moderacaoService;

    @GetMapping
    public ResponseEntity<List<OfertaResponseDTO>> listarMediacoes() {

        List<OfertaResponseDTO> lista = moderacaoService.listarOfertasEmMediacao();
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/{id}/resolver")
    public ResponseEntity<OfertaResponseDTO> resolverConflito(
            @PathVariable UUID id,
            @RequestParam DecisaoMediacao decisao) throws Exception {

        OfertaResponseDTO ofertaResolvida = moderacaoService.resolverConflito(id, decisao);
        return ResponseEntity.ok(ofertaResolvida);
    }


    @GetMapping("/todas")
    public ResponseEntity<Page<OfertaResponseDTO>> listarTodasAsOfertas(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "dataCriacao") String orderBy,
            @RequestParam(defaultValue = "DESC") String direction){

        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.valueOf(direction), orderBy);
        Page<OfertaResponseDTO> resultado = moderacaoService.listarTodasAsOfertas(pageRequest);

        return ResponseEntity.ok(resultado);
    }


    @DeleteMapping("/{id}/cancelar")
    public ResponseEntity<OfertaResponseDTO> cancelarManual(
            @PathVariable UUID id,
            @RequestParam boolean forcarReembolso) throws Exception {

        OfertaResponseDTO resultado = moderacaoService.cancelarManual(id, forcarReembolso);

        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/estatisticas/lucro")
    public ResponseEntity<BigDecimal> verLucro() {
        java.math.BigDecimal resultado = moderacaoService.calcularLucroPlataforma();
        return ResponseEntity.ok(resultado);
    }

}
