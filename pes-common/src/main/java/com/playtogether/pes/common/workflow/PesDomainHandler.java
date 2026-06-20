package com.playtogether.pes.common.workflow;

import com.playtogether.pes.common.model.EntityType;
import com.playtogether.pes.common.model.PesDomainMessage;

/**
 * 도메인(LOT/WF/DURABLE) 단위 메시지 처리기.
 * 각 도메인 서비스가 이 인터페이스를 구현하면 {@link PesDomainRouter} 가 자동 등록한다.
 *
 * @param <M> 처리 대상 도메인 메시지 타입
 */
public interface PesDomainHandler<M extends PesDomainMessage> {

    /** 이 핸들러가 담당하는 도메인. */
    EntityType entityType();

    /** 메시지의 workflow 를 처리하고 결과를 반환한다. */
    PesProcessResult handle(M message);
}
