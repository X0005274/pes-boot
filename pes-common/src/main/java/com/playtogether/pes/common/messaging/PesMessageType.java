package com.playtogether.pes.common.messaging;

import com.playtogether.pes.common.model.EntityType;
import com.playtogether.pes.common.model.PesDomainMessage;

/**
 * 도메인이 인바운드 메시징 계층에 자신을 등록하기 위한 SPI.
 * 각 도메인 모듈이 구현 빈을 등록하면, 메시징 계층이 subject → 메시지 클래스 매핑을 자동 구성한다.
 * (예: LOT → PES.BIZ.LOT.EVENT → LotMessage.class)
 */
public interface PesMessageType {

    EntityType entityType();

    /** 이 도메인 메시지를 수신할 inbound subject (Biz 레이어 기준). */
    String inboundSubject();

    /** 처리 결과/상태 변경을 push 할 outbound 이벤트 subject (UI 레이어 기준). */
    String eventSubject();

    /** 페이로드(JSON) 를 역직렬화할 도메인 메시지 클래스. */
    Class<? extends PesDomainMessage> messageClass();
}
