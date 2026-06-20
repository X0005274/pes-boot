package com.playtogether.pes.hub.jdbc;

import com.playtogether.pes.common.model.PesDomainMessage;
import com.playtogether.pes.common.model.PesEventInfo;
import com.playtogether.pes.common.model.PesMeta;
import com.playtogether.pes.durable.api.DurableMessage;
import com.playtogether.pes.durable.api.DurableStepOptions;
import com.playtogether.pes.durable.api.DurableWorkflowStep;
import com.playtogether.pes.hub.HubAck;
import com.playtogether.pes.hub.HubAckSource;
import com.playtogether.pes.hub.PartitionedHubSource;
import com.playtogether.pes.hub.jdbc.cursor.PesHubCursorService;
import com.playtogether.pes.lot.api.LotMessage;
import com.playtogether.pes.lot.api.LotStepOptions;
import com.playtogether.pes.lot.api.LotWorkflowStep;
import com.playtogether.pes.wf.api.WfMessage;
import com.playtogether.pes.wf.api.WfStepOptions;
import com.playtogether.pes.wf.api.WfWorkflowStep;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HIS 기반 증분 replay 소스(at-least-once, 파티션=도메인). HubDB 의 *_HIS 를 TIMEKEY 순서로 읽어
 * 각 이벤트를 단일 step 메시지로 재생한다. 워터마크는 영속 커서(PES DB)에 저장한다.
 *
 * <p><b>at-least-once</b>: poll 시점엔 커서를 전진시키지 않고, 처리 결과 ack 를 받아
 * 도메인별 "성공 연속 구간(contiguous prefix)"까지만 커서를 전진시킨다.
 *
 * <p><b>파티션 병렬</b>: 도메인(LOT/WF/DURABLE)을 파티션으로 노출해 병렬 적재를 지원한다.
 * 미확정 이벤트 맵은 correlationId(전역 유일) 키의 ConcurrentHashMap 이라 파티션 병렬 polling/ack 에 안전하다.
 */
public class HistoryHubSource implements PartitionedHubSource, HubAckSource {

    private static final int MAX_LIMIT = 1000;
    private static final String PART_LOT = "LOT";
    private static final String PART_WF = "WF";
    private static final String PART_DURABLE = "DURABLE";
    private static final String LOT_CURSOR = "his:LOT";
    private static final String WF_CURSOR = "his:WF";
    private static final String DURABLE_CURSOR = "his:DURABLE";

    private final JdbcTemplate hubJdbc;
    private final PesHubCursorService cursors;

    /** 미확정 이벤트: correlationId(전역 유일) → (커서키, timekey). ack 시 제거. */
    private final Map<String, PendingEvent> pending = new ConcurrentHashMap<>();

    public HistoryHubSource(JdbcTemplate hubJdbc, PesHubCursorService cursors) {
        this.hubJdbc = hubJdbc;
        this.cursors = cursors;
    }

    @Override
    public List<String> partitions() {
        return List.of(PART_LOT, PART_WF, PART_DURABLE);
    }

    @Override
    public List<PesDomainMessage> poll(String partition, int maxBatch) {
        int limit = clamp(maxBatch);
        return switch (partition) {
            case PART_LOT -> pollLot(limit);
            case PART_WF -> pollWf(limit);
            case PART_DURABLE -> pollDurable(limit);
            default -> List.of();
        };
    }

    @Override
    public List<PesDomainMessage> poll(int maxBatch) {
        int limit = clamp(maxBatch);
        List<PesDomainMessage> out = new ArrayList<>();
        out.addAll(pollLot(limit));
        out.addAll(pollWf(limit));
        out.addAll(pollDurable(limit));
        return out;
    }

    @Override
    public void acknowledge(List<HubAck> acks) {
        // ack 순서(=파티션별 TIMEKEY 순서)대로 보며, 도메인별 성공 연속 구간까지만 커서 전진
        Map<String, String> advanceTo = new HashMap<>();
        Set<String> broken = new HashSet<>();
        for (HubAck ack : acks) {
            PendingEvent event = (ack.correlationId() != null)
                    ? this.pending.remove(ack.correlationId()) : null;
            if (event == null || broken.contains(event.cursorKey())) {
                continue;
            }
            if (ack.success()) {
                advanceTo.put(event.cursorKey(), event.timekey());
            } else {
                broken.add(event.cursorKey());
            }
        }
        advanceTo.forEach(this.cursors::save);
    }

    private List<PesDomainMessage> pollLot(int limit) {
        List<HisRow> rows = queryHis("PES_LOT_HIS", "LOT_ID", this.cursors.load(LOT_CURSOR), limit);
        List<PesDomainMessage> messages = new ArrayList<>(rows.size());
        for (HisRow row : rows) {
            LotStepOptions options = LotStepOptions.empty();
            if ("changeSpec".equals(row.method())) {
                String spec = specOf("PES_LOT_MAS", "LOT_SPEC", "LOT_ID", row.id());
                if (spec != null) {
                    options = new LotStepOptions(Map.of("lotSpec", spec));
                }
            }
            String correlationId = "HUB-LOT-" + row.id() + "-" + row.timekey();
            LotWorkflowStep step = new LotWorkflowStep(row.method(), options, event(row));
            messages.add(new LotMessage("LOT", row.id(), null, null, List.of(step),
                    meta(row, correlationId)));
            this.pending.put(correlationId, new PendingEvent(LOT_CURSOR, row.timekey()));
        }
        return messages;
    }

    private List<PesDomainMessage> pollWf(int limit) {
        List<HisRow> rows = queryHis("PES_WF_HIS", "WF_ID", this.cursors.load(WF_CURSOR), limit);
        List<PesDomainMessage> messages = new ArrayList<>(rows.size());
        for (HisRow row : rows) {
            WfStepOptions options = WfStepOptions.empty();
            if ("changeSpec".equals(row.method())) {
                String spec = specOf("PES_WF_MAS", "WF_SPEC", "WF_ID", row.id());
                if (spec != null) {
                    options = new WfStepOptions(Map.of("wfSpec", spec));
                }
            }
            String correlationId = "HUB-WF-" + row.id() + "-" + row.timekey();
            WfWorkflowStep step = new WfWorkflowStep(row.method(), options, event(row));
            messages.add(new WfMessage("WF", row.id(), null, List.of(step),
                    meta(row, correlationId)));
            this.pending.put(correlationId, new PendingEvent(WF_CURSOR, row.timekey()));
        }
        return messages;
    }

    private List<PesDomainMessage> pollDurable(int limit) {
        List<HisRow> rows = queryHis("PES_DURABLE_HIS", "DURABLE_ID", this.cursors.load(DURABLE_CURSOR), limit);
        List<PesDomainMessage> messages = new ArrayList<>(rows.size());
        for (HisRow row : rows) {
            DurableStepOptions options = DurableStepOptions.empty();
            if ("changeSpec".equals(row.method())) {
                String spec = specOf("PES_DURABLE_MAS", "DURABLE_SPEC", "DURABLE_ID", row.id());
                if (spec != null) {
                    options = new DurableStepOptions(Map.of("durableSpec", spec));
                }
            }
            String correlationId = "HUB-DUR-" + row.id() + "-" + row.timekey();
            DurableWorkflowStep step = new DurableWorkflowStep(row.method(), options, event(row));
            messages.add(new DurableMessage("DURABLE", row.id(), null, List.of(step),
                    meta(row, correlationId)));
            this.pending.put(correlationId, new PendingEvent(DURABLE_CURSOR, row.timekey()));
        }
        return messages;
    }

    private List<HisRow> queryHis(String table, String idColumn, String watermark, int limit) {
        String sql = "SELECT " + idColumn + " AS DID, TIMEKEY, METHOD, EVENT_CD, EVENT_TM,"
                + " EVENT_DESC, STAT_TYP, USER_ID FROM " + table
                + " WHERE (? IS NULL OR TIMEKEY > ?) ORDER BY TIMEKEY FETCH FIRST " + limit + " ROWS ONLY";
        return this.hubJdbc.query(sql, (rs, rowNum) -> new HisRow(
                rs.getString("DID"), rs.getString("TIMEKEY"), rs.getString("METHOD"),
                rs.getString("EVENT_CD"), rs.getString("EVENT_TM"), rs.getString("EVENT_DESC"),
                rs.getString("STAT_TYP"), rs.getString("USER_ID")), watermark, watermark);
    }

    private String specOf(String table, String specColumn, String idColumn, String id) {
        try {
            List<String> result = this.hubJdbc.query(
                    "SELECT " + specColumn + " FROM " + table + " WHERE " + idColumn + " = ?",
                    (rs, rowNum) -> rs.getString(1), id);
            return result.isEmpty() ? null : result.get(0);
        } catch (org.springframework.dao.DataAccessException ex) {
            // MAS 미존재/조회 실패 시 보정 불가 → null (changeSpec 은 처리 단계에서 FAILED 처리됨)
            return null;
        }
    }

    private static int clamp(int maxBatch) {
        return Math.max(1, Math.min(maxBatch, MAX_LIMIT));
    }

    private static PesEventInfo event(HisRow row) {
        return new PesEventInfo(row.eventCd(), row.eventTm(), row.eventDesc(), row.statTyp());
    }

    private static PesMeta meta(HisRow row, String correlationId) {
        return new PesMeta("HUB", row.userId(), correlationId, null, null);
    }

    private record HisRow(String id, String timekey, String method, String eventCd,
                          String eventTm, String eventDesc, String statTyp, String userId) {
    }

    private record PendingEvent(String cursorKey, String timekey) {
    }
}
