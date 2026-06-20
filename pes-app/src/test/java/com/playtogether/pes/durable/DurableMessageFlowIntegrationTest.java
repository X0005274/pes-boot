package com.playtogether.pes.durable;

import com.playtogether.pes.durable.entity.PesDurableMas;
import com.playtogether.pes.durable.repository.PesDurableHisRepository;
import com.playtogether.pes.durable.repository.PesDurableMasRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DURABLE 도메인 end-to-end 통합 테스트. PesDomainRouter 가 DURABLE 메시지를 라우팅하는지 포함.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DurableMessageFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PesDurableMasRepository masRepository;

    @Autowired
    private PesDurableHisRepository hisRepository;

    @Test
    void createdThenMakeInUseWithBindLot_persistsMasAndTwoHistoryRows() throws Exception {
        String durableId = "DUR-IT-1";
        String body = """
                {
                  "entityType": "DURABLE",
                  "durableId": "%s",
                  "lotId": "LOT-9",
                  "workflow": [
                    {
                      "method": "created",
                      "options": {},
                      "event": { "eventCd": "CREATED", "statTyp": "NEW" }
                    },
                    {
                      "method": "makeInUse",
                      "options": { "bindLot": true, "syncToEquipment": true },
                      "event": { "eventCd": "INUSE", "statTyp": "IN_USE" }
                    }
                  ],
                  "meta": { "srcSystem": "UI", "userId": "U1" }
                }
                """.formatted(durableId);

        this.mockMvc.perform(post("/durable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityType").value("DURABLE"))
                .andExpect(jsonPath("$.entityId").value(durableId))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.steps.length()").value(2))
                .andExpect(jsonPath("$.steps[1].method").value("makeInUse"))
                .andExpect(jsonPath("$.steps[1].status").value("SUCCESS"));

        Optional<PesDurableMas> mas = this.masRepository.findById(durableId);
        assertThat(mas).isPresent();
        assertThat(mas.get().getStatTyp()).isEqualTo("IN_USE");
        assertThat(mas.get().getLotId()).isEqualTo("LOT-9");

        assertThat(this.hisRepository.findByIdDurableIdOrderByIdTimekeyDesc(durableId)).hasSize(2);
    }

    @Test
    void changeSpec_updatesDurableSpecFromOptions() throws Exception {
        String durableId = "DUR-IT-SPEC";
        String body = """
                {
                  "entityType": "DURABLE",
                  "durableId": "%s",
                  "workflow": [
                    { "method": "created", "options": {}, "event": { "eventCd": "CREATED", "statTyp": "NEW" } },
                    { "method": "changeSpec", "options": { "durableSpec": "DSPEC-1" }, "event": { "eventCd": "CHG", "statTyp": "NEW" } }
                  ],
                  "meta": { "srcSystem": "UI" }
                }
                """.formatted(durableId);

        this.mockMvc.perform(post("/durable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.steps[1].method").value("changeSpec"));

        Optional<PesDurableMas> mas = this.masRepository.findById(durableId);
        assertThat(mas).isPresent();
        assertThat(mas.get().getDurableSpec()).isEqualTo("DSPEC-1");
    }

    @Test
    void invalidEntityType_returns400() throws Exception {
        String body = """
                {
                  "entityType": "LOT",
                  "durableId": "DUR-IT-BAD",
                  "workflow": [
                    { "method": "created", "options": {}, "event": { "eventCd": "CREATED" } }
                  ],
                  "meta": { "srcSystem": "UI" }
                }
                """;

        this.mockMvc.perform(post("/durable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
