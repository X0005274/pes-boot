package com.playtogether.pes.common.query;

import com.playtogether.pes.common.persistence.PesEventInfoEmbedded;
import com.playtogether.pes.common.persistence.PesMetaEmbedded;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * *_HIS 한 건의 조회 뷰. LOT/WF/DURABLE 공통(엔티티 노출 대신 사용).
 */
@Schema(description = "이력 1건")
public record PesHistoryEntry(
        String timekey,
        String method,
        String eventCd,
        String eventTm,
        String eventDesc,
        String statTyp,
        String srcSystem,
        String userId,
        String correlationId
) {
    public static PesHistoryEntry of(String timekey, String method,
                                     PesEventInfoEmbedded event, PesMetaEmbedded meta) {
        return new PesHistoryEntry(
                timekey,
                method,
                event != null ? event.getEventCd() : null,
                event != null ? event.getEventTm() : null,
                event != null ? event.getEventDesc() : null,
                event != null ? event.getStatTyp() : null,
                meta != null ? meta.getSrcSystem() : null,
                meta != null ? meta.getUserId() : null,
                meta != null ? meta.getCorrelationId() : null);
    }
}
