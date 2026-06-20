package com.playtogether.pes.durable.repository;

import com.playtogether.pes.durable.entity.PesDurableHis;
import com.playtogether.pes.durable.entity.PesDurableHisId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DURABLE 이력 리포지토리. PK = (DURABLE_ID, TIMEKEY).
 */
@Repository
public interface PesDurableHisRepository extends JpaRepository<PesDurableHis, PesDurableHisId> {

    List<PesDurableHis> findByIdDurableIdOrderByIdTimekeyDesc(String durableId);

    @Query("SELECT h FROM PesDurableHis h WHERE h.id.durableId = :durableId"
            + " AND (:method IS NULL OR h.method = :method)"
            + " AND (:fromTk IS NULL OR h.id.timekey >= :fromTk)"
            + " AND (:toTk IS NULL OR h.id.timekey <= :toTk)"
            + " ORDER BY h.id.timekey DESC")
    Page<PesDurableHis> search(@Param("durableId") String durableId,
                              @Param("method") String method,
                              @Param("fromTk") String fromTimekey,
                              @Param("toTk") String toTimekey,
                              Pageable pageable);
}
