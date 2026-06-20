package com.playtogether.pes.idempotency.repository;

import com.playtogether.pes.idempotency.entity.PesProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 멱등 마커 리포지토리. PK = correlationId(String).
 */
@Repository
public interface PesProcessedMessageRepository extends JpaRepository<PesProcessedMessage, String> {
}
