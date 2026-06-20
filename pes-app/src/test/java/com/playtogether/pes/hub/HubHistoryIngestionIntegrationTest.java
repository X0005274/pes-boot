package com.playtogether.pes.hub;

import com.playtogether.pes.durable.entity.PesDurableMas;
import com.playtogether.pes.durable.repository.PesDurableMasRepository;
import com.playtogether.pes.hub.jdbc.cursor.PesHubCursorRepository;
import com.playtogether.pes.lot.entity.PesLotMas;
import com.playtogether.pes.lot.repository.PesLotMasRepository;
import com.playtogether.pes.wf.repository.PesWfMasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HIS 기반 증분 replay + 영속 워터마크 커서 검증.
 */
@SpringBootTest(properties = {
        "pes.hub.enabled=true",
        "pes.hub.mode=his",
        "pes.hub.datasource.url=jdbc:h2:mem:hubhis;MODE=Oracle;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "pes.hub.datasource.username=sa",
        "pes.hub.datasource.password=",
        "pes.hub.datasource.driver-class-name=org.h2.Driver"
})
class HubHistoryIngestionIntegrationTest {

    private static final String HUB_URL =
            "jdbc:h2:mem:hubhis;MODE=Oracle;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    @Autowired
    private HubIngestionService ingestionService;

    @Autowired
    private PesLotMasRepository lotMasRepository;

    @Autowired
    private PesWfMasRepository wfMasRepository;

    @Autowired
    private PesDurableMasRepository durableMasRepository;

    @Autowired
    private PesHubCursorRepository cursorRepository;

    private JdbcTemplate hub;

    @BeforeEach
    void seedHubHistory() {
        DriverManagerDataSource ds = new DriverManagerDataSource(HUB_URL, "sa", "");
        ds.setDriverClassName("org.h2.Driver");
        this.hub = new JdbcTemplate(ds);

        for (String table : new String[]{"PES_LOT_HIS", "PES_WF_HIS", "PES_DURABLE_HIS"}) {
            String idCol = table.equals("PES_LOT_HIS") ? "LOT_ID"
                    : table.equals("PES_WF_HIS") ? "WF_ID" : "DURABLE_ID";
            this.hub.execute("CREATE TABLE IF NOT EXISTS " + table + " ("
                    + idCol + " VARCHAR(40), TIMEKEY VARCHAR(24), METHOD VARCHAR(30),"
                    + " EVENT_CD VARCHAR(30), EVENT_TM VARCHAR(20), EVENT_DESC VARCHAR(200),"
                    + " STAT_TYP VARCHAR(20), SRC_SYSTEM VARCHAR(20), USER_ID VARCHAR(30),"
                    + " CORR_ID VARCHAR(60), REQUEST_TM VARCHAR(20), LOCALE VARCHAR(20),"
                    + " PRIMARY KEY (" + idCol + ", TIMEKEY))");
            this.hub.update("DELETE FROM " + table);
        }
        // 결정적 시작: 영속 커서 초기화
        this.cursorRepository.deleteAll();

        this.hub.update("INSERT INTO PES_LOT_HIS (LOT_ID, TIMEKEY, METHOD, EVENT_CD, STAT_TYP, USER_ID) "
                + "VALUES ('LOT-H1','000000000000000001','created','CREATED','NEW','HUBUSER')");
        this.hub.update("INSERT INTO PES_WF_HIS (WF_ID, TIMEKEY, METHOD, EVENT_CD, STAT_TYP, USER_ID) "
                + "VALUES ('WF-H1','000000000000000001','created','CREATED','NEW','HUBUSER')");
        this.hub.update("INSERT INTO PES_DURABLE_HIS (DURABLE_ID, TIMEKEY, METHOD, EVENT_CD, STAT_TYP, USER_ID) "
                + "VALUES ('DUR-H1','000000000000000001','created','CREATED','NEW','HUBUSER')");
        this.hub.update("INSERT INTO PES_DURABLE_HIS (DURABLE_ID, TIMEKEY, METHOD, EVENT_CD, STAT_TYP, USER_ID) "
                + "VALUES ('DUR-H1','000000000000000002','makeInUse','INUSE','IN_USE','HUBUSER')");
    }

    @Test
    void replaysHisEvents_reconstructingMasState() {
        HubIngestResult result = this.ingestionService.ingest(100);

        assertThat(result.total()).isEqualTo(4);
        assertThat(result.failed()).isZero();

        assertThat(this.lotMasRepository.findById("LOT-H1")).isPresent();
        assertThat(this.wfMasRepository.findById("WF-H1")).isPresent();

        Optional<PesDurableMas> durable = this.durableMasRepository.findById("DUR-H1");
        assertThat(durable).isPresent();
        assertThat(durable.get().getStatTyp()).isEqualTo("IN_USE");

        // 커서가 영속됨
        assertThat(this.cursorRepository.findById("his:LOT")).isPresent();
        assertThat(this.cursorRepository.findById("his:DURABLE").get().getWatermark())
                .isEqualTo("000000000000000002");
    }

    @Test
    void atLeastOnce_failedEvent_blocksCursorAdvanceAndRetriesUntilSuccess() {
        // LOT 이벤트 추가: created(0001, 이미 시드됨) 다음에 changeSpec(0002) — MAS 에 LOT_SPEC 없어 실패 유도
        // (changeSpec 은 spec 이 없으면 FAILED → 커서가 0001 까지만 전진, 0002 는 재시도 대상)
        this.hub.update("INSERT INTO PES_LOT_HIS (LOT_ID, TIMEKEY, METHOD, EVENT_CD, STAT_TYP, USER_ID) "
                + "VALUES ('LOT-H1','000000000000000002','changeSpec','CHG','NEW','HUBUSER')");

        HubIngestResult first = this.ingestionService.ingest(100);
        // LOT: created 성공 + changeSpec 실패. WF created, DURABLE created/makeInUse 성공.
        assertThat(first.failed()).isEqualTo(1);

        // LOT 커서는 created(0001)까지만 전진(실패한 0002 앞)
        assertThat(this.cursorRepository.findById("his:LOT").get().getWatermark())
                .isEqualTo("000000000000000001");

        // HubDB 의 changeSpec 이 처리 가능하도록 LOT_SPEC 보정(= 이제 spec 조회 성공)
        this.hub.execute("CREATE TABLE IF NOT EXISTS PES_LOT_MAS ("
                + "LOT_ID VARCHAR(40) PRIMARY KEY, LOT_SPEC VARCHAR(200))");
        this.hub.update("MERGE INTO PES_LOT_MAS (LOT_ID, LOT_SPEC) KEY(LOT_ID) VALUES ('LOT-H1','SPEC-FIX')");

        // 재적재 → 재시도된 changeSpec(0002) 성공, 커서 전진
        HubIngestResult retry = this.ingestionService.ingest(100);
        assertThat(retry.failed()).isZero();
        assertThat(this.cursorRepository.findById("his:LOT").get().getWatermark())
                .isEqualTo("000000000000000002");

        PesLotMas lot = this.lotMasRepository.findById("LOT-H1").orElseThrow();
        assertThat(lot.getLotSpec()).isEqualTo("SPEC-FIX");
    }

    @Test
    void concurrentIngest_isSerializedByLock_eventsProcessedOnce() throws Exception {
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<HubIngestResult> task = () -> this.ingestionService.ingest(100);
            java.util.concurrent.Future<HubIngestResult> f1 = pool.submit(task);
            java.util.concurrent.Future<HubIngestResult> f2 = pool.submit(task);

            HubIngestResult r1 = f1.get(30, java.util.concurrent.TimeUnit.SECONDS);
            HubIngestResult r2 = f2.get(30, java.util.concurrent.TimeUnit.SECONDS);

            // 비관적 잠금으로 직렬화 → 두 ingest 의 total 합이 4(이벤트 수). 잠금 없으면 8(중복 재조회)이 됨.
            assertThat(r1.total() + r2.total()).isEqualTo(4);
            assertThat(r1.failed() + r2.failed()).isZero();
        } finally {
            pool.shutdownNow();
        }

        // 상태는 정확히 한 번 적용
        assertThat(this.lotMasRepository.findById("LOT-H1")).isPresent();
        assertThat(this.wfMasRepository.findById("WF-H1")).isPresent();
        PesDurableMas durable = this.durableMasRepository.findById("DUR-H1").orElseThrow();
        assertThat(durable.getStatTyp()).isEqualTo("IN_USE");

        // 도메인별 잠금 키가 사용됨(병렬 잠금 분리)
        assertThat(this.cursorRepository.findById("his:ingest-LOT")).isPresent();
        assertThat(this.cursorRepository.findById("his:ingest-WF")).isPresent();
        assertThat(this.cursorRepository.findById("his:ingest-DURABLE")).isPresent();
    }

    @Test
    void persistentCursor_resumesIncrementally_afterReingest() {
        // 1차 적재 → 커서 전진
        HubIngestResult first = this.ingestionService.ingest(100);
        assertThat(first.total()).isEqualTo(4);

        // 재적재(새 이벤트 없음) → 커서 이후라 0건
        HubIngestResult repeat = this.ingestionService.ingest(100);
        assertThat(repeat.total()).isZero();

        // 새 HIS 이벤트 추가(LOT released, 더 큰 TIMEKEY)
        this.hub.update("INSERT INTO PES_LOT_HIS (LOT_ID, TIMEKEY, METHOD, EVENT_CD, STAT_TYP, USER_ID) "
                + "VALUES ('LOT-H1','000000000000000003','released','RELEASED','REL','HUBUSER')");

        // 증분 적재 → 새 이벤트 1건만
        HubIngestResult incremental = this.ingestionService.ingest(100);
        assertThat(incremental.total()).isEqualTo(1);

        Optional<PesLotMas> lot = this.lotMasRepository.findById("LOT-H1");
        assertThat(lot).isPresent();
        assertThat(lot.get().getStatTyp()).isEqualTo("REL");
    }
}
