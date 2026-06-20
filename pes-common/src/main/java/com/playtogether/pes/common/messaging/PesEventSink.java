package com.playtogether.pes.common.messaging;

import com.playtogether.pes.common.model.EntityType;
import com.playtogether.pes.common.workflow.PesProcessResult;

/**
 * 도메인 처리 결과를 UI 레이어로 push 하기 위한 아웃바운드 SPI.
 * 메시징 계층이 구현하며, 도메인 서비스는 이 인터페이스만 의존한다(전송 무관).
 * 파생 도메인(LOT→WF/DURABLE 등)도 각자 서비스를 거치며 자기 이벤트를 발행한다.
 */
public interface PesEventSink {

    /** 해당 도메인의 이벤트 subject 로 처리 결과를 발행한다. */
    void publishResult(EntityType domain, PesProcessResult result);
}
