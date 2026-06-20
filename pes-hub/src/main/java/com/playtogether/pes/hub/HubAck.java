package com.playtogether.pes.hub;

/**
 * 한 메시지의 처리 결과 통지.
 *
 * @param correlationId 메시지 correlationId(소스가 이벤트를 식별하는 키)
 * @param success       처리 성공 여부
 */
public record HubAck(String correlationId, boolean success) {
}
