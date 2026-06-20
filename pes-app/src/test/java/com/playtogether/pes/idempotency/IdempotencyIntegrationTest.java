package com.playtogether.pes.idempotency;

import com.playtogether.pes.lot.repository.PesLotMasRepository;
import com.playtogether.pes.messaging.transport.InMemoryPesMessageTransport;
import com.playtogether.pes.messaging.transport.PesMessageTransport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * correlationId 기반 인바운드 멱등성 검증.
 * 동일 correlationId 의 중복 수신은 재처리 없이 이전 결과를 반환해야 한다.
 */
@SpringBootTest
class IdempotencyIntegrationTest {

    @Autowired
    private PesMessageTransport transport;

    @Autowired
    private PesLotMasRepository lotMasRepository;

    private InMemoryPesMessageTransport memory() {
        return (InMemoryPesMessageTransport) this.transport;
    }

    private String createdMessage(String lotId, String correlationId) {
        return """
                {
                  "entityType": "LOT",
                  "lotId": "%s",
                  "workflow": [
                    { "method": "created", "options": {}, "event": { "eventCd": "CREATED", "statTyp": "NEW" } }
                  ],
                  "meta": { "srcSystem": "UI", "correlationId": "%s" }
                }
                """.formatted(lotId, correlationId);
    }

    @Test
    void duplicateCorrelationId_isNotReprocessed() {
        String correlationId = "TX-IDEMP-DUP";

        // 1) 최초 수신 → LOT-IDEMP-1 생성, 성공
        String firstReply = memory().simulateInbound(
                "PES.BIZ.LOT.EVENT", createdMessage("LOT-IDEMP-1", correlationId));
        assertThat(firstReply).contains("\"status\":\"SUCCESS\"");
        assertThat(this.lotMasRepository.findById("LOT-IDEMP-1")).isPresent();

        // 2) 동일 correlationId 로 재수신(다른 lotId) → 무시되고 이전 결과 반환
        String secondReply = memory().simulateInbound(
                "PES.BIZ.LOT.EVENT", createdMessage("LOT-IDEMP-2", correlationId));

        // 이전 결과(LOT-IDEMP-1)가 반환되고, 두 번째 lotId 는 처리되지 않음
        assertThat(secondReply).contains("LOT-IDEMP-1");
        assertThat(secondReply).doesNotContain("LOT-IDEMP-2");
        assertThat(this.lotMasRepository.findById("LOT-IDEMP-2")).isEmpty();
    }

    @Test
    void differentCorrelationId_isProcessedNormally() {
        memory().simulateInbound("PES.BIZ.LOT.EVENT", createdMessage("LOT-IDEMP-A", "TX-IDEMP-A"));
        memory().simulateInbound("PES.BIZ.LOT.EVENT", createdMessage("LOT-IDEMP-B", "TX-IDEMP-B"));

        assertThat(this.lotMasRepository.findById("LOT-IDEMP-A")).isPresent();
        assertThat(this.lotMasRepository.findById("LOT-IDEMP-B")).isPresent();
    }
}
