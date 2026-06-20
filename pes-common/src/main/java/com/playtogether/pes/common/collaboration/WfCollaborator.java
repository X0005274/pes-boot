package com.playtogether.pes.common.collaboration;

import com.playtogether.pes.common.model.PesMeta;

/**
 * WF 도메인이 다른 도메인에 제공하는 협력 API. WF 모듈이 구현 빈을 등록한다.
 * (예: LOT created + createWf 옵션 → LOT 에서 파생 WF 생성)
 */
public interface WfCollaborator {

    /** 주어진 LOT 에 연계된 WF 를 파생 생성한다. */
    void createWfForLot(String wfId, String lotId, PesMeta meta);
}
