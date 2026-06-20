package com.playtogether.pes.idempotency;

import com.playtogether.pes.lot.repository.PesLotMasRepository;
import com.playtogether.pes.messaging.transport.InMemoryPesMessageTransport;
import com.playtogether.pes.messaging.transport.PesMessageTransport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * correlationId 자동 생성 검증: 인바운드에 correlationId 가 없으면 서버가 발급하고,
 * 응답에 포함된다. 발급 ID 는 매 메시지 고유하므로 멱등 중복으로 오인되지 않는다.
 */
@SpringBootTest
class CorrelationIdAutoGenIntegrationTest {

    @Autowired
    private PesMessageTransport transport;

    @Autowired
    private PesLotMasRepository lotMasRepository;

    private InMemoryPesMessageTransport memory() {
        return (InMemoryPesMessageTransport) this.transport;
    }

    private String createdMessageWithoutCorrId(String lotId) {
        return """
                {
                  "entityType": "LOT",
                  "lotId": "%s",
                  "workflow": [
                    { "method": "created", "options": {}, "event": { "eventCd": "CREATED", "statTyp": "NEW" } }
                  ],
                  "meta": { "srcSystem": "UI" }
                }
                """.formatted(lotId);
    }

    @Test
    void missingCorrelationId_isServerGenerated_andReturnedInReply() {
        String reply = memory().simulateInbound(
                "PES.BIZ.LOT.EVENT", createdMessageWithoutCorrId("LOT-AUTOID"));

        assertThat(reply).contains("\"status\":\"SUCCESS\"");
        // 서버 발급 correlationId 가 응답에 포함됨 (PES- 접두)
        assertThat(reply).contains("\"correlationId\":\"PES-");
        assertThat(this.lotMasRepository.findById("LOT-AUTOID")).isPresent();
    }

    @Test
    void missingCorrelationId_messagesAreNotDeduplicated() {
        // correlationId 없는 서로 다른 메시지는 각각 고유 ID 를 받아 모두 처리됨
        memory().simulateInbound("PES.BIZ.LOT.EVENT", createdMessageWithoutCorrId("LOT-AUTO-1"));
        memory().simulateInbound("PES.BIZ.LOT.EVENT", createdMessageWithoutCorrId("LOT-AUTO-2"));

        assertThat(this.lotMasRepository.findById("LOT-AUTO-1")).isPresent();
        assertThat(this.lotMasRepository.findById("LOT-AUTO-2")).isPresent();
    }
}
