package com.playtogether.pes.hub;

import com.playtogether.pes.common.model.PesDomainMessage;
import com.playtogether.pes.common.model.PesEventInfo;
import com.playtogether.pes.common.model.PesMeta;
import com.playtogether.pes.lot.api.LotMessage;
import com.playtogether.pes.lot.api.LotStepOptions;
import com.playtogether.pes.lot.api.LotWorkflowStep;
import com.playtogether.pes.lot.entity.PesLotMas;
import com.playtogether.pes.lot.repository.PesLotMasRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HubDB 적재 경로 검증: HubSource 의 레코드가 동일 Biz 파이프라인으로 적재되고
 * srcSystem=HUB 로 기록되며, 재적재는 멱등 처리로 중복되지 않는다.
 */
@SpringBootTest
class HubIngestionIntegrationTest {

    @TestConfiguration
    static class TestHubSourceConfig {
        @Bean
        @Primary
        HubSource testHubSource() {
            return maxBatch -> List.<PesDomainMessage>of(
                    hubLot("HUB-LOT-1"),
                    hubLot("HUB-LOT-2"));
        }

        private static LotMessage hubLot(String lotId) {
            LotWorkflowStep step = new LotWorkflowStep(
                    "created",
                    LotStepOptions.empty(),
                    new PesEventInfo("CREATED", null, "from HubDB", "NEW"));
            // srcSystem=HUB, correlationId 안정적(재적재 멱등)
            return new LotMessage("LOT", lotId, null, null, List.of(step),
                    new PesMeta("HUB", null, "HUB-" + lotId, null, null));
        }
    }

    @Autowired
    private HubIngestionService ingestionService;

    @Autowired
    private PesLotMasRepository lotMasRepository;

    @Test
    void ingest_loadsHubRecordsViaBizPipeline_withSrcSystemHub() {
        HubIngestResult result = this.ingestionService.ingest(100);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.success()).isEqualTo(2);
        assertThat(result.failed()).isZero();

        Optional<PesLotMas> lot1 = this.lotMasRepository.findById("HUB-LOT-1");
        assertThat(lot1).isPresent();
        assertThat(lot1.get().getSrcSystem()).isEqualTo("HUB");
        assertThat(this.lotMasRepository.findById("HUB-LOT-2")).isPresent();
    }

    @Test
    void reIngest_isIdempotent_noDuplicateState() {
        this.ingestionService.ingest(100);
        long countAfterFirst = this.lotMasRepository.count();

        // 동일 레코드 재적재 → 멱등(중복 무시), 성공으로 보고되나 상태는 그대로
        HubIngestResult second = this.ingestionService.ingest(100);
        assertThat(second.success()).isEqualTo(2);
        assertThat(this.lotMasRepository.count()).isEqualTo(countAfterFirst);
    }
}
