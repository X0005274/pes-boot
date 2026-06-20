package com.playtogether.pes.common.support;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * *_HIS 의 TIMEKEY 및 이벤트 타임스탬프를 서버에서 생성한다.
 * TIMEKEY = "yyyyMMddHHmmssSSS"(17) + 프로세스 내 시퀀스(6) = 23자 (컬럼 길이 24 이내).
 * 동일 밀리초 내 다중 step 의 순서/유일성을 시퀀스로 보장한다.
 */
@Component
public class PesTimekeyGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private static final long SEQ_BOUND = 1_000_000L;

    private final AtomicLong sequence = new AtomicLong(0L);

    /** HIS PK 용 TIMEKEY. */
    public String next() {
        long seq = Math.floorMod(this.sequence.getAndIncrement(), SEQ_BOUND);
        return currentTimestamp() + String.format("%06d", seq);
    }

    /** MAS create/update 시각, event 시각 보정 등에 쓰는 타임스탬프. */
    public String currentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }
}
