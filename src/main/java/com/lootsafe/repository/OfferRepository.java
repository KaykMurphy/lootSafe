package com.lootsafe.repository;

import com.lootsafe.enums.TransactionStatus;
import com.lootsafe.model.Offer;
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
public interface OfferRepository extends JpaRepository<Offer, UUID> {

    Optional<Offer> findByMercadoPagoPaymentId(Long mercadoPagoPaymentId);

    List<Offer> findAllByTransactionStatusAndReleaseDeadlineBefore(
            TransactionStatus status,
            LocalDateTime deadline
    );

    List<Offer> findAllByTransactionStatus(TransactionStatus status);


    @Query("SELECT SUM(o.platformFee) FROM Offer o WHERE o.transactionStatus IN :statuses")
    BigDecimal calculateTotalPlatformProfitByStatuses(@Param("statuses") List<TransactionStatus> statuses);

    List<Offer> findAllByTransactionStatusAndCreatedAtBefore(
            TransactionStatus status,
            LocalDateTime deadline
    );
}
