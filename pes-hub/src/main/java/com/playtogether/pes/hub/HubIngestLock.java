package com.playtogether.pes.hub;

import java.util.function.Supplier;

/**
 * ingest 직렬화용 잠금 SPI. lockKey 단위로 직렬화하므로, 도메인별 키를 쓰면
 * 서로 다른 도메인은 병렬로, 같은 도메인은 직렬로 적재된다.
 * 구현이 없으면(예: in-memory) 잠금 없이 그대로 실행한다.
 */
public interface HubIngestLock {

    <T> T runExclusively(String lockKey, Supplier<T> work);
}
