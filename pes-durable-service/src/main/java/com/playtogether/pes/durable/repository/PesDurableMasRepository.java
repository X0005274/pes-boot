package com.playtogether.pes.durable.repository;

import com.playtogether.pes.durable.entity.PesDurableMas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * DURABLE 마스터 리포지토리. PK = DURABLE_ID(String).
 */
@Repository
public interface PesDurableMasRepository extends JpaRepository<PesDurableMas, String> {
}
