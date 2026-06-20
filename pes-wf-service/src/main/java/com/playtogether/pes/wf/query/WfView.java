package com.playtogether.pes.wf.query;

import com.playtogether.pes.wf.entity.PesWfMas;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * WF 현재 상태 조회 뷰(PES_WF_MAS).
 */
@Schema(description = "WF 현재 상태")
public record WfView(
        String wfId,
        String lotId,
        String statTyp,
        String wfSpec,
        String srcSystem,
        String userId,
        String createTm,
        String updateTm
) {
    public static WfView from(PesWfMas mas) {
        return new WfView(
                mas.getWfId(), mas.getLotId(), mas.getStatTyp(), mas.getWfSpec(),
                mas.getSrcSystem(), mas.getUserId(), mas.getCreateTm(), mas.getUpdateTm());
    }
}
