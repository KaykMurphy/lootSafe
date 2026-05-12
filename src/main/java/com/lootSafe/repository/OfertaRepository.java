package com.lootSafe.repository;

import com.lootSafe.enums.StatusTransacao;
import com.lootSafe.model.Oferta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfertaRepository extends JpaRepository<Oferta, UUID> {
    Optional<Oferta> findByMercadoPagoId(Long mercadoPagoId);

    List<Oferta> findAllByStatusTransacaoAndDataLimiteLiberacaoBefore(
            StatusTransacao status,
            LocalDateTime dataLimite
    );

    List<Oferta> findAllByStatusTransacao(StatusTransacao status);

    @Query("SELECT SUM(o.taxaPlataforma) FROM Oferta o WHERE o.statusTransacao = :status")
    BigDecimal calcularLucroTotalPorStatus(@Param("status") StatusTransacao status);

    List<Oferta> findAllByStatusTransacaoAndDataCriacaoBefore(
            StatusTransacao status,
            LocalDateTime dataLimite
    );
}
