package com.github.benimarushiimon.cardpayment.repository;

import com.github.benimarushiimon.cardpayment.Entity.TransferLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransferLogRepository extends JpaRepository<TransferLogEntity, Long> {
    Optional<TransferLogEntity> findByOperationId(String operationId);
}
