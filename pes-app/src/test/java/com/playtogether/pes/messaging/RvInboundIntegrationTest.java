package com.playtogether.pes.messaging;

import com.playtogether.pes.durable.repository.PesDurableMasRepository;
import com.playtogether.pes.lot.repository.PesLotMasRepository;
import com.playtogether.pes.messaging.transport.InMemoryPesMessageTransport;
import com.playtogether.pes.messaging.transport.PesMessageTransport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인바운드 메시징 파이프라인 통합 테스트.
 * in-memory 전송으로 RV 수신을 시뮬레이션 → 디스패처 → 라우터 → 도메인 처리 → reply 확인.
 */
@SpringBootTest
class RvInboundIntegrationTest {

    @Autowired
    private PesMessageTransport transport;

    @Autowired
    private PesLotMasRepository lotMasRepository;

    @Autowired
    private PesDurableMasRepository durableMasRepository;

    private InMemoryPesMessageTransport memory() {
        // 실제 RV 미설치 환경 → in-memory 전송이 기본 빈으로 주입됨
        return (InMemoryPesMessageTransport) this.transport;
    }

    @Test
    void inboundLotMessage_routedAndPersisted_withReply() {
        String json = """
                {
                  "entityType": "LOT",
                  "lotId": "LOT-RV-1",
                  "durableId": "DUR-RV-1",
                  "workflow": [
                    {
                      "method": "created",
                      "options": { "createDurable": true },
                      "event": { "eventCd": "CREATED", "statTyp": "NEW" }
                    }
                  ],
                  "meta": { "srcSystem": "UI", "userId": "U1" }
                }
                """;

        String reply = memory().simulateInbound("PES.BIZ.LOT.EVENT", json);

        assertThat(reply).contains("\"status\":\"SUCCESS\"");
        assertThat(reply).contains("\"entityId\":\"LOT-RV-1\"");
        assertThat(this.lotMasRepository.findById("LOT-RV-1")).isPresent();
        // createDurable 연계까지 인바운드 경로로 동작
        assertThat(this.durableMasRepository.findById("DUR-RV-1")).isPresent();

        // 아웃바운드: 성공 처리 후 PES.UI.LOT.EVENT 로 상태 변경 push
        assertThat(memory().publishedEvents())
                .anyMatch(event -> event.subject().equals("PES.UI.LOT.EVENT")
                        && event.payload().contains("\"status\":\"SUCCESS\"")
                        && event.payload().contains("LOT-RV-1"));

        // 파생 도메인 push: createDurable 로 생성된 DURABLE 도 PES.UI.DURABLE.EVENT 발행
        assertThat(memory().publishedEvents())
                .anyMatch(event -> event.subject().equals("PES.UI.DURABLE.EVENT")
                        && event.payload().contains("\"status\":\"SUCCESS\"")
                        && event.payload().contains("DUR-RV-1"));
    }

    @Test
    void inboundFailure_doesNotPublishEvent() {
        int before = memory().publishedEvents().size();
        // 존재하지 않는 LOT 에 released → 처리 결과 FAILED → push 안 함
        String json = """
                {
                  "entityType": "LOT",
                  "lotId": "LOT-RV-NOEVENT",
                  "workflow": [ { "method": "released", "options": {}, "event": { "eventCd": "RELEASED", "statTyp": "REL" } } ],
                  "meta": { "srcSystem": "UI" }
                }
                """;

        String reply = memory().simulateInbound("PES.BIZ.LOT.EVENT", json);

        assertThat(reply).contains("\"status\":\"FAILED\"");
        assertThat(memory().publishedEvents())
                .noneMatch(event -> event.payload().contains("LOT-RV-NOEVENT"));
        assertThat(memory().publishedEvents()).hasSize(before);
    }

    @Test
    void inboundInvalidEntityType_repliesFailed() {
        String json = """
                {
                  "entityType": "WF",
                  "lotId": "LOT-RV-BAD",
                  "workflow": [ { "method": "created", "options": {}, "event": { "eventCd": "CREATED" } } ],
                  "meta": { "srcSystem": "UI" }
                }
                """;

        String reply = memory().simulateInbound("PES.BIZ.LOT.EVENT", json);

        // entityType=WF 가 LotMessage 계약(@AssertTrue) 위반 → 검증 실패 reply
        assertThat(reply).contains("\"status\":\"FAILED\"");
        assertThat(this.lotMasRepository.findById("LOT-RV-BAD")).isEmpty();
    }

    @Test
    void inboundUnknownSubject_repliesFailed() {
        String reply = memory().simulateInbound("PES.BIZ.WF.EVENT", "not-relevant");
        // WF subject 는 구독되어 있으나 payload 가 깨졌으므로 역직렬화 실패
        assertThat(reply).contains("\"status\":\"FAILED\"");
    }
}
