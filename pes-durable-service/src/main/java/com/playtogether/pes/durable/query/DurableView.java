package com.playtogether.pes.durable.query;

import com.playtogether.pes.durable.entity.PesDurableMas;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DURABLE 현재 상태 조회 뷰(PES_DURABLE_MAS).
 */
@Schema(description = "DURABLE 현재 상태")
public record DurableView(
        String durableId,
        String lotId,
        String statTyp,
        String durableSpec,
        String srcSystem,
        String userId,
        String createTm,
        String updateTm
) {
    public static DurableView from(PesDurableMas mas) {
        return new DurableView(
                mas.getDurableId(), mas.getLotId(), mas.getStatTyp(), mas.getDurableSpec(),
                mas.getSrcSystem(), mas.getUserId(), mas.getCreateTm(), mas.getUpdateTm());
    }
}
