package com.playtogether.pes.lot.repository;

import com.playtogether.pes.lot.entity.PesLotHis;
import com.playtogether.pes.lot.entity.PesLotHisId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * LOT 이력 리포지토리. PK = (LOT_ID, TIMEKEY).
 */
@Repository
public interface PesLotHisRepository extends JpaRepository<PesLotHis, PesLotHisId> {

    /** 특정 LOT 의 이력을 TIMEKEY 내림차순(최신 우선)으로 조회. */
    List<PesLotHis> findByIdLotIdOrderByIdTimekeyDesc(String lotId);

    /** 필터(method/TIMEKEY 범위) + 페이징 조회. 최신순. */
    @Query("SELECT h FROM PesLotHis h WHERE h.id.lotId = :lotId"
            + " AND (:method IS NULL OR h.method = :method)"
            + " AND (:fromTk IS NULL OR h.id.timekey >= :fromTk)"
            + " AND (:toTk IS NULL OR h.id.timekey <= :toTk)"
            + " ORDER BY h.id.timekey DESC")
    Page<PesLotHis> search(@Param("lotId") String lotId,
                           @Param("method") String method,
                           @Param("fromTk") String fromTimekey,
                           @Param("toTk") String toTimekey,
                           Pageable pageable);
}
