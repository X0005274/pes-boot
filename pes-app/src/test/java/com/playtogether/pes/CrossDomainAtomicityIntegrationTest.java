package com.playtogether.pes;

import com.playtogether.pes.durable.repository.PesDurableMasRepository;
import com.playtogether.pes.lot.repository.PesLotMasRepository;
import com.playtogether.pes.messaging.transport.InMemoryPesMessageTransport;
import com.playtogether.pes.messaging.transport.PesMessageTransport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 크로스 도메인 원자성 검증: 파생 도메인(DURABLE) 생성 후 LOT step 이 실패하면,
 * 본/파생 변경이 모두 롤백되고 어떤 이벤트도 발행되지 않아야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CrossDomainAtomicityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PesLotMasRepository lotMasRepository;

    @Autowired
    private PesDurableMasRepository durableMasRepository;

    @Autowired
    private PesMessageTransport transport;

    private InMemoryPesMessageTransport memory() {
        return (InMemoryPesMessageTransport) this.transport;
    }

    @Test
    void derivedDurableCreated_thenLotStepFails_rollsBackEverything() throws Exception {
        String lotId = "LOT-ATOM";
        String durableId = "DUR-ATOM";
        int eventsBefore = memory().publishedEvents().size();

        // created(createDurable=true) 성공 → 이어서 changeSpec(lotSpec 없음) 실패 → 전체 롤백
        String body = """
                {
                  "entityType": "LOT",
                  "lotId": "%s",
                  "durableId": "%s",
                  "workflow": [
                    {
                      "method": "created",
                      "options": { "createDurable": true },
                      "event": { "eventCd": "CREATED", "statTyp": "NEW" }
                    },
                    {
                      "method": "changeSpec",
                      "options": {},
                      "event": { "eventCd": "CHG", "statTyp": "NEW" }
                    }
                  ],
                  "meta": { "srcSystem": "UI" }
                }
                """.formatted(lotId, durableId);

        this.mockMvc.perform(post("/lot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.steps[0].method").value("created"))
                .andExpect(jsonPath("$.steps[1].method").value("changeSpec"))
                .andExpect(jsonPath("$.steps[1].status").value("FAILED"));

        // 원자성: LOT 도 DURABLE 도 적재되지 않음(파생 생성까지 롤백)
        assertThat(this.lotMasRepository.findById(lotId)).isEmpty();
        assertThat(this.durableMasRepository.findById(durableId)).isEmpty();

        // 롤백되었으므로 어떤 이벤트도 발행되지 않음
        assertThat(memory().publishedEvents())
                .noneMatch(event -> event.payload().contains(lotId) || event.payload().contains(durableId));
        assertThat(memory().publishedEvents()).hasSize(eventsBefore);
    }
}
