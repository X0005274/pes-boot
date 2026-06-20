package com.playtogether.pes.common.model;

/**
 * LOT/WF/DURABLE 메시지 DTO 가 공통 라우터/핸들러에서 다뤄질 수 있도록 하는 마커 인터페이스.
 * 도메인 메시지는 자신의 EntityType 과 공통 메타데이터를 노출해야 한다.
 */
public interface PesDomainMessage {

    /** 메시지가 속한 도메인. (entityType 문자열을 EntityType 으로 해석) */
    EntityType resolveEntityType();

    /** 도메인 식별자 (lotId / wfId / durableId). 로깅·관측·idempotency 등에 사용. */
    String entityId();

    /** 공통 메타데이터. */
    PesMeta meta();

    /** correlationId 를 교체한 메시지 사본 반환(서버 발급 시 사용). */
    PesDomainMessage withCorrelationId(String correlationId);
}
