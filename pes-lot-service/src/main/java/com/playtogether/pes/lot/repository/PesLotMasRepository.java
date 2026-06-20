package com.playtogether.pes.lot.repository;

import com.playtogether.pes.lot.entity.PesLotMas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * LOT 마스터 리포지토리. PK = LOT_ID(String).
 */
@Repository
public interface PesLotMasRepository extends JpaRepository<PesLotMas, String> {
}
