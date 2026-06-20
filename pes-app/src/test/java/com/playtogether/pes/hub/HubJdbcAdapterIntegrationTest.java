package com.playtogether.pes.hub;

import com.playtogether.pes.lot.entity.PesLotMas;
import com.playtogether.pes.lot.repository.PesLotMasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 2번째 DataSource(HubDB) 어댑터 검증.
 * 별도 H2(mem:hubadapter)를 HubDB 로 사용해 JdbcHubSource 가 읽어 PES 로 적재하는지 확인한다.
 * 기본 PES DataSource(autoconfig)는 그대로 유지됨을 함께 검증한다.
 */
@SpringBootTest(properties = {
        "pes.hub.enabled=true",
        "pes.hub.datasource.url=jdbc:h2:mem:hubadapter;MODE=Oracle;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "pes.hub.datasource.username=sa",
        "pes.hub.datasource.password=",
        "pes.hub.datasource.driver-class-name=org.h2.Driver"
})
class HubJdbcAdapterIntegrationTest {

    private static final String HUB_URL =
            "jdbc:h2:mem:hubadapter;MODE=Oracle;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    @Autowired
    private HubIngestionService ingestionService;

    @Autowired
    private PesLotMasRepository lotMasRepository;

    @BeforeEach
    void seedHubDatabase() {
        DriverManagerDataSource ds = new DriverManagerDataSource(HUB_URL, "sa", "");
        ds.setDriverClassName("org.h2.Driver");
        JdbcTemplate hub = new JdbcTemplate(ds);
        hub.execute("""
                CREATE TABLE IF NOT EXISTS PES_LOT_MAS (
                    LOT_ID VARCHAR(40) PRIMARY KEY,
                    WF_ID VARCHAR(40),
                    DURABLE_ID VARCHAR(40),
                    STAT_TYP VARCHAR(20),
                    LOT_SPEC VARCHAR(200),
                    SRC_SYSTEM VARCHAR(20),
                    USER_ID VARCHAR(30),
                    CREATE_TM VARCHAR(20),
                    UPDATE_TM VARCHAR(20),
                    VER_NO NUMBER
                )
                """);
        hub.update("DELETE FROM PES_LOT_MAS");
        hub.update("INSERT INTO PES_LOT_MAS (LOT_ID, STAT_TYP, USER_ID) VALUES (?,?,?)",
                "HUBSRC-1", "NEW", "HUBUSER");
        hub.update("INSERT INTO PES_LOT_MAS (LOT_ID, STAT_TYP, USER_ID) VALUES (?,?,?)",
                "HUBSRC-2", "NEW", "HUBUSER");
    }

    @Test
    void ingestsFromRealHubDataSource_intoPes_withSrcSystemHub() {
        HubIngestResult result = this.ingestionService.ingest(100);

        assertThat(result.total()).isGreaterThanOrEqualTo(2);
        assertThat(result.failed()).isZero();

        Optional<PesLotMas> lot1 = this.lotMasRepository.findById("HUBSRC-1");
        assertThat(lot1).isPresent();
        assertThat(lot1.get().getSrcSystem()).isEqualTo("HUB");
        assertThat(lot1.get().getStatTyp()).isEqualTo("NEW");
        assertThat(this.lotMasRepository.findById("HUBSRC-2")).isPresent();
    }
}
