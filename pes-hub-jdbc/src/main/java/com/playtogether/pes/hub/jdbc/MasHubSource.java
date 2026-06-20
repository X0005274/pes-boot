package com.playtogether.pes.hub.jdbc;

import com.playtogether.pes.common.model.PesDomainMessage;
import com.playtogether.pes.common.model.PesEventInfo;
import com.playtogether.pes.common.model.PesMeta;
import com.playtogether.pes.hub.HubSource;
import com.playtogether.pes.lot.api.LotMessage;
import com.playtogether.pes.lot.api.LotStepOptions;
import com.playtogether.pes.lot.api.LotWorkflowStep;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MAS 기반 초기 적재 소스. HubDB 의 PES_LOT_MAS 를 LOT_ID 워터마크로 페이징하며
 * 'created' LOT 메시지(srcSystem=HUB)로 매핑한다. correlationId 는 LOT 기준 안정값(재적재 멱등).
 */
public class MasHubSource implements HubSource {

    private static final int MAX_LIMIT = 1000;

    private final JdbcTemplate hubJdbc;
    private final AtomicReference<String> lastLotId = new AtomicReference<>(null);

    public MasHubSource(JdbcTemplate hubJdbc) {
        this.hubJdbc = hubJdbc;
    }

    @Override
    public List<PesDomainMessage> poll(int maxBatch) {
        int limit = Math.max(1, Math.min(maxBatch, MAX_LIMIT));
        String from = this.lastLotId.get();

        String sql = "SELECT LOT_ID, WF_ID, DURABLE_ID, STAT_TYP, USER_ID"
                + " FROM PES_LOT_MAS WHERE (? IS NULL OR LOT_ID > ?)"
                + " ORDER BY LOT_ID FETCH FIRST " + limit + " ROWS ONLY";

        List<PesDomainMessage> messages = this.hubJdbc.query(sql,
                (rs, rowNum) -> toCreatedMessage(
                        rs.getString("LOT_ID"), rs.getString("WF_ID"), rs.getString("DURABLE_ID"),
                        rs.getString("STAT_TYP"), rs.getString("USER_ID")),
                from, from);

        if (!messages.isEmpty()) {
            this.lastLotId.set(messages.get(messages.size() - 1).entityId());
        }
        return messages;
    }

    private PesDomainMessage toCreatedMessage(String lotId, String wfId, String durableId,
                                              String statTyp, String userId) {
        LotWorkflowStep step = new LotWorkflowStep(
                "created", LotStepOptions.empty(),
                new PesEventInfo("CREATED", null, "loaded from HubDB", statTyp));
        return new LotMessage("LOT", lotId, wfId, durableId, List.of(step),
                new PesMeta("HUB", userId, "HUB-LOT-" + lotId, null, null));
    }
}
