package com.playtogether.pes.common.collaboration;

import com.playtogether.pes.common.model.PesMeta;

import java.util.Optional;

/**
 * LOT 도메인이 다른 도메인에 제공하는 협력 API. LOT 모듈이 구현 빈을 등록한다.
 * (예: WF created + inheritLotSpec → LOT spec 조회, WF changeSpec + applyToLot → LOT 전파)
 */
public interface LotCollaborator {

    /** LOT 의 현재 spec 을 조회한다. 없으면 empty. */
    Optional<String> findLotSpec(String lotId);

    /** 주어진 spec 을 LOT 에 changeSpec 으로 적용한다. */
    void applySpecToLot(String lotId, String spec, PesMeta meta);
}
