package com.playtogether.pes.hub.jdbc.cursor;

import com.playtogether.pes.hub.HubIngestLock;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * DB 비관적 잠금 기반 ingest 직렬화. lockKey 행을 PESSIMISTIC_WRITE 로 잠근 채 작업을 수행한다.
 * 도메인별 키("his:ingest-LOT" 등)를 쓰면 서로 다른 도메인은 병렬, 같은 도메인은 직렬화된다.
 *
 * <p>잠금 트랜잭션이 적재 전체를 감싸지만, 메시지 처리는 REQUIRES_NEW 로 독립 커밋되어
 * 멱등/at-least-once 시맨틱이 유지된다.
 */
@Component
@ConditionalOnProperty(prefix = "pes.hub", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class JdbcHubIngestLock implements HubIngestLock {

    private final PesHubCursorRepository repository;
    private final PesHubCursorService cursorService;

    @Override
    @Transactional
    public <T> T runExclusively(String lockKey, Supplier<T> work) {
        ensureLockRow(lockKey);
        this.repository.findAndLock(lockKey);
        return work.get();
    }

    private void ensureLockRow(String lockKey) {
        try {
            // REQUIRES_NEW 예외를 트랜잭션 경계 밖에서 잡아 현재 잠금 트랜잭션 오염을 방지
            this.cursorService.createRowIfAbsent(lockKey);
        } catch (DataAccessException concurrentCreate) {
            // 다른 인스턴스가 동시에 생성함 — 무시(행은 존재하게 됨)
        }
    }
}
