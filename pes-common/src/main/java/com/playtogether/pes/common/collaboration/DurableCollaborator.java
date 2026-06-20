package com.playtogether.pes.common.collaboration;

import com.playtogether.pes.common.model.PesMeta;

/**
 * DURABLE 도메인이 다른 도메인에 제공하는 협력 API. DURABLE 모듈이 구현 빈을 등록한다.
 * (예: LOT created + createDurable, LOT released + makeDurableInUse)
 */
public interface DurableCollaborator {

    /** 주어진 LOT 에 연계된 DURABLE 을 파생 생성한다. */
    void createDurableForLot(String durableId, String lotId, PesMeta meta);

    /** DURABLE 을 InUse 로 전이하고 LOT 에 바인딩한다. */
    void makeDurableInUse(String durableId, String lotId, PesMeta meta);
}
