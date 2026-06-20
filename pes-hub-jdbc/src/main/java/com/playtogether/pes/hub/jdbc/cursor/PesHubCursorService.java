package com.playtogether.pes.hub.jdbc.cursor;

import com.playtogether.pes.common.support.PesTimekeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hub 워터마크 커서 영속 서비스(PES DB). 도메인별 마지막 처리 TIMEKEY 를 저장/조회한다.
 */
@Service
@RequiredArgsConstructor
public class PesHubCursorService {

    private final PesHubCursorRepository repository;
    private final PesTimekeyGenerator timekeyGenerator;

    /**
     * 잠금/커서 행이 없으면 생성(독립 트랜잭션). 동시 생성 경합 시 제약 위반 예외를 던지므로
     * 호출자가 트랜잭션 경계 밖에서 잡아야 한다(여기서 잡으면 tx 가 rollback-only 가 됨).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createRowIfAbsent(String cursorKey) {
        if (this.repository.findById(cursorKey).isEmpty()) {
            this.repository.save(new PesHubCursor(cursorKey));
        }
    }

    @Transactional(readOnly = true)
    public String load(String cursorKey) {
        return this.repository.findById(cursorKey)
                .map(PesHubCursor::getWatermark)
                .orElse(null);
    }

    @Transactional
    public void save(String cursorKey, String watermark) {
        PesHubCursor cursor = this.repository.findById(cursorKey)
                .orElseGet(() -> new PesHubCursor(cursorKey));
        cursor.setWatermark(watermark);
        cursor.setUpdateTm(this.timekeyGenerator.currentTimestamp());
        this.repository.save(cursor);
    }
}
