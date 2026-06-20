package com.playtogether.pes.messaging;

import com.playtogether.pes.lot.repository.PesLotMasRepository;
import com.playtogether.pes.messaging.transport.InMemoryPesMessageTransport;
import com.playtogether.pes.messaging.transport.PesMessageTransport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PES.UI 포워더 검증: UI.REQUEST 수신 → 검증 → BIZ.EVENT 전달 → 응답 릴레이.
 * (한 컨텍스트에 포워더 + Biz 디스패처를 함께 두고 in-memory 전송으로 2-hop 시뮬레이션)
 */
@SpringBootTest(properties = "pes.forwarder.enabled=true")
class PesUiForwarderIntegrationTest {

    @Autowired
    private PesMessageTransport transport;

    @Autowired
    private PesLotMasRepository lotMasRepository;

    private InMemoryPesMessageTransport memory() {
        return (InMemoryPesMessageTransport) this.transport;
    }

    @Test
    void uiRequest_isValidated_forwardedToBiz_andPersisted() {
        String json = """
                {
                  "entityType": "LOT",
                  "lotId": "LOT-FWD-1",
                  "workflow": [
                    { "method": "created", "options": {}, "event": { "eventCd": "CREATED", "statTyp": "NEW" } }
                  ],
                  "meta": { "srcSystem": "UI", "correlationId": "TX-FWD-1" }
                }
                """;

        // UI 요청 subject 로 수신 → 포워더가 BIZ 로 전달 → 처리 결과 릴레이
        String reply = memory().simulateInbound("PES.UI.LOT.REQUEST", json);

        assertThat(reply).contains("\"status\":\"SUCCESS\"");
        assertThat(reply).contains("LOT-FWD-1");
        assertThat(this.lotMasRepository.findById("LOT-FWD-1")).isPresent();
    }

    @Test
    void uiRequest_invalid_isRejected_notForwarded() {
        // entityType 이 LOT 계약 위반(WF) → 포워더 검증 실패 → Biz 미전달, 미적재
        String json = """
                {
                  "entityType": "WF",
                  "lotId": "LOT-FWD-BAD",
                  "workflow": [ { "method": "created", "options": {}, "event": { "eventCd": "CREATED" } } ],
                  "meta": { "srcSystem": "UI" }
                }
                """;

        String reply = memory().simulateInbound("PES.UI.LOT.REQUEST", json);

        assertThat(reply).contains("\"status\":\"FAILED\"");
        assertThat(this.lotMasRepository.findById("LOT-FWD-BAD")).isEmpty();
    }
}
