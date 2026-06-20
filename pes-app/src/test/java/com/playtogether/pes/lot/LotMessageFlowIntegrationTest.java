package com.playtogether.pes.lot;

import com.playtogether.pes.lot.entity.PesLotHis;
import com.playtogether.pes.lot.entity.PesLotMas;
import com.playtogether.pes.lot.repository.PesLotHisRepository;
import com.playtogether.pes.lot.repository.PesLotMasRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /lot 진입점 ~ DB 적재까지의 end-to-end 통합 테스트 (H2).
 */
@SpringBootTest
@AutoConfigureMockMvc
class LotMessageFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PesLotMasRepository masRepository;

    @Autowired
    private PesLotHisRepository hisRepository;

    @Test
    void createdThenReleased_persistsMasAndTwoHistoryRows() throws Exception {
        String lotId = "LOT-IT-REL";
        String body = """
                {
                  "entityType": "LOT",
                  "lotId": "%s",
                  "wfId": "WF-1",
                  "durableId": "DUR-1",
                  "workflow": [
                    {
                      "method": "created",
                      "options": {},
                      "event": { "eventCd": "CREATED", "eventDesc": "from UI", "statTyp": "NEW" }
                    },
                    {
                      "method": "released",
                      "options": {},
                      "event": { "eventCd": "RELEASED", "eventDesc": "released", "statTyp": "REL" }
                    }
                  ],
                  "meta": { "srcSystem": "UI", "userId": "U1", "correlationId": "TX-1" }
                }
                """.formatted(lotId);

        this.mockMvc.perform(post("/lot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityType").value("LOT"))
                .andExpect(jsonPath("$.entityId").value(lotId))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.steps.length()").value(2))
                .andExpect(jsonPath("$.steps[0].method").value("created"))
                .andExpect(jsonPath("$.steps[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.steps[1].method").value("released"))
                .andExpect(jsonPath("$.steps[1].status").value("SUCCESS"));

        Optional<PesLotMas> mas = this.masRepository.findById(lotId);
        assertThat(mas).isPresent();
        assertThat(mas.get().getStatTyp()).isEqualTo("REL");
        assertThat(mas.get().getDurableId()).isEqualTo("DUR-1");
        assertThat(mas.get().getCreateTm()).isNotBlank();
        assertThat(mas.get().getUpdateTm()).isNotBlank();

        List<PesLotHis> history = this.hisRepository.findByIdLotIdOrderByIdTimekeyDesc(lotId);
        assertThat(history).hasSize(2);
        // 최신 우선 정렬 → 첫 행이 released
        assertThat(history.get(0).getMethod()).isEqualTo("released");
        assertThat(history.get(1).getMethod()).isEqualTo("created");
        // eventTm 서버 보정 확인
        assertThat(history.get(0).getEvent().getEventTm()).isNotBlank();
    }

    @Test
    void changeSpec_updatesLotSpecFromOptions() throws Exception {
        String lotId = "LOT-IT-SPEC";
        String body = """
                {
                  "entityType": "LOT",
                  "lotId": "%s",
                  "workflow": [
                    {
                      "method": "created",
                      "options": {},
                      "event": { "eventCd": "CREATED", "statTyp": "NEW" }
                    },
                    {
                      "method": "changeSpec",
                      "options": { "lotSpec": "SPEC-X" },
                      "event": { "eventCd": "CHG", "statTyp": "NEW" }
                    }
                  ],
                  "meta": { "srcSystem": "UI" }
                }
                """.formatted(lotId);

        this.mockMvc.perform(post("/lot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.steps[1].method").value("changeSpec"))
                .andExpect(jsonPath("$.steps[1].status").value("SUCCESS"));

        Optional<PesLotMas> mas = this.masRepository.findById(lotId);
        assertThat(mas).isPresent();
        assertThat(mas.get().getLotSpec()).isEqualTo("SPEC-X");
    }

    @Test
    void releasedWithoutCreated_returnsFailedResult() throws Exception {
        String lotId = "LOT-IT-NOEXIST";
        String body = """
                {
                  "entityType": "LOT",
                  "lotId": "%s",
                  "workflow": [
                    {
                      "method": "released",
                      "options": {},
                      "event": { "eventCd": "RELEASED", "statTyp": "REL" }
                    }
                  ],
                  "meta": { "srcSystem": "UI" }
                }
                """.formatted(lotId);

        this.mockMvc.perform(post("/lot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.steps[0].status").value("FAILED"));

        assertThat(this.masRepository.findById(lotId)).isEmpty();
    }

    @Test
    void invalidEntityType_returns400() throws Exception {
        String body = """
                {
                  "entityType": "WF",
                  "lotId": "LOT-IT-BAD",
                  "workflow": [
                    { "method": "created", "options": {}, "event": { "eventCd": "CREATED" } }
                  ],
                  "meta": { "srcSystem": "UI" }
                }
                """;

        this.mockMvc.perform(post("/lot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyWorkflow_returns400() throws Exception {
        String body = """
                {
                  "entityType": "LOT",
                  "lotId": "LOT-IT-EMPTY",
                  "workflow": [],
                  "meta": { "srcSystem": "UI" }
                }
                """;

        this.mockMvc.perform(post("/lot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
