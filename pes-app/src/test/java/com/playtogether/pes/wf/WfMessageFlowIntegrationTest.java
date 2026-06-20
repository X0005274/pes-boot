package com.playtogether.pes.wf;

import com.playtogether.pes.wf.entity.PesWfMas;
import com.playtogether.pes.wf.repository.PesWfHisRepository;
import com.playtogether.pes.wf.repository.PesWfMasRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WF 도메인 end-to-end 통합 테스트. PesDomainRouter 가 WF 메시지를 WfMessageService 로 라우팅하는지 포함.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WfMessageFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PesWfMasRepository masRepository;

    @Autowired
    private PesWfHisRepository hisRepository;

    @Test
    void createdThenChangeSpec_persistsMasAndTwoHistoryRows() throws Exception {
        String wfId = "WF-IT-1";
        String body = """
                {
                  "entityType": "WF",
                  "wfId": "%s",
                  "lotId": "LOT-1",
                  "workflow": [
                    {
                      "method": "created",
                      "options": {},
                      "event": { "eventCd": "CREATED", "statTyp": "NEW" }
                    },
                    {
                      "method": "changeSpec",
                      "options": { "wfSpec": "WSPEC-1" },
                      "event": { "eventCd": "CHG", "statTyp": "NEW" }
                    }
                  ],
                  "meta": { "srcSystem": "UI", "userId": "U1" }
                }
                """.formatted(wfId);

        this.mockMvc.perform(post("/wf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityType").value("WF"))
                .andExpect(jsonPath("$.entityId").value(wfId))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.steps.length()").value(2))
                .andExpect(jsonPath("$.steps[1].method").value("changeSpec"))
                .andExpect(jsonPath("$.steps[1].status").value("SUCCESS"));

        Optional<PesWfMas> mas = this.masRepository.findById(wfId);
        assertThat(mas).isPresent();
        assertThat(mas.get().getLotId()).isEqualTo("LOT-1");
        assertThat(mas.get().getWfSpec()).isEqualTo("WSPEC-1");

        assertThat(this.hisRepository.findByIdWfIdOrderByIdTimekeyDesc(wfId)).hasSize(2);
    }

    @Test
    void invalidEntityType_returns400() throws Exception {
        String body = """
                {
                  "entityType": "LOT",
                  "wfId": "WF-IT-BAD",
                  "workflow": [
                    { "method": "created", "options": {}, "event": { "eventCd": "CREATED" } }
                  ],
                  "meta": { "srcSystem": "UI" }
                }
                """;

        this.mockMvc.perform(post("/wf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
