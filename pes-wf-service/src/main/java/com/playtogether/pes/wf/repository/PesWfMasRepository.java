package com.playtogether.pes.wf.repository;

import com.playtogether.pes.wf.entity.PesWfMas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * WF 마스터 리포지토리. PK = WF_ID(String).
 */
@Repository
public interface PesWfMasRepository extends JpaRepository<PesWfMas, String> {
}
