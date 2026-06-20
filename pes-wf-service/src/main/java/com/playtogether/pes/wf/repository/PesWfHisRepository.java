package com.playtogether.pes.wf.repository;

import com.playtogether.pes.wf.entity.PesWfHis;
import com.playtogether.pes.wf.entity.PesWfHisId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * WF 이력 리포지토리. PK = (WF_ID, TIMEKEY).
 */
@Repository
public interface PesWfHisRepository extends JpaRepository<PesWfHis, PesWfHisId> {

    List<PesWfHis> findByIdWfIdOrderByIdTimekeyDesc(String wfId);

    @Query("SELECT h FROM PesWfHis h WHERE h.id.wfId = :wfId"
            + " AND (:method IS NULL OR h.method = :method)"
            + " AND (:fromTk IS NULL OR h.id.timekey >= :fromTk)"
            + " AND (:toTk IS NULL OR h.id.timekey <= :toTk)"
            + " ORDER BY h.id.timekey DESC")
    Page<PesWfHis> search(@Param("wfId") String wfId,
                          @Param("method") String method,
                          @Param("fromTk") String fromTimekey,
                          @Param("toTk") String toTimekey,
                          Pageable pageable);
}
