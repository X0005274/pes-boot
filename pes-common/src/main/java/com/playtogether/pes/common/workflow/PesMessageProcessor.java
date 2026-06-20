package com.playtogether.pes.common.workflow;

import com.playtogether.pes.common.model.PesDomainMessage;

/**
 * 인바운드 메시지 처리 진입 추상화. 기본 구현은 단순 라우팅이며,
 * idempotency 모듈이 있으면 중복 방지 데코레이터가 @Primary 로 대체한다.
 * 파생(협력자) 호출은 이 진입을 거치지 않으므로 멱등 검사 대상이 아니다.
 */
public interface PesMessageProcessor {

    PesProcessResult process(PesDomainMessage message);
}
