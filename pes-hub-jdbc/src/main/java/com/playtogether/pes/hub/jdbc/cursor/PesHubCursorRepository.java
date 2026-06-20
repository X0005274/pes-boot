package com.playtogether.pes.hub.jdbc.cursor;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Hub 워터마크 커서 리포지토리. PK = cursorKey(String).
 */
@Repository
public interface PesHubCursorRepository extends JpaRepository<PesHubCursor, String> {

    /** 비관적 쓰기 잠금으로 행을 조회(동시 ingest 직렬화). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM PesHubCursor c WHERE c.cursorKey = :key")
    Optional<PesHubCursor> findAndLock(@Param("key") String key);
}
