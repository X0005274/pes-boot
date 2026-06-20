package com.playtogether.pes.lot.query;

import com.playtogether.pes.lot.entity.PesLotMas;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * LOT 현재 상태 조회 뷰(PES_LOT_MAS).
 */
@Schema(description = "LOT 현재 상태")
public record LotView(
        String lotId,
        String wfId,
        String durableId,
        String statTyp,
        String lotSpec,
        String srcSystem,
        String userId,
        String createTm,
        String updateTm
) {
    public static LotView from(PesLotMas mas) {
        return new LotView(
                mas.getLotId(), mas.getWfId(), mas.getDurableId(), mas.getStatTyp(),
                mas.getLotSpec(), mas.getSrcSystem(), mas.getUserId(),
                mas.getCreateTm(), mas.getUpdateTm());
    }
}
