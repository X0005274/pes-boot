package com.playtogether.pes.hub;

import java.util.List;

/**
 * 처리 결과를 받아 워터마크를 전진시키는 소스(at-least-once).
 * HubSource 가 이 인터페이스를 함께 구현하면, HubIngestionService 가 배치 처리 후
 * 결과를 ack 한다. 구현체는 도메인별 "성공 연속 구간"까지만 커서를 전진시켜
 * 실패 이벤트가 건너뛰어지지 않게 한다(재시도 + 멱등 = at-least-once).
 */
public interface HubAckSource {

    void acknowledge(List<HubAck> acks);
}
