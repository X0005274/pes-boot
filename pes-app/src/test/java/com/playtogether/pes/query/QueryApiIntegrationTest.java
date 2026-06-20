package com.playtogether.pes.query;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 조회 API(상태/이력) 통합 테스트.
 */
@SpringBootTest
@AutoConfigureMockMvc
class QueryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private void postLot(String lotId) throws Exception {
        String body = """
                {
                  "entityType": "LOT",
                  "lotId": "%s",
                  "workflow": [
                    { "method": "created", "options": {}, "event": { "eventCd": "CREATED", "statTyp": "NEW" } },
                    { "method": "changeSpec", "options": { "lotSpec": "SP-Q" }, "event": { "eventCd": "CHG", "statTyp": "NEW" } }
                  ],
                  "meta": { "srcSystem": "UI", "correlationId": "TX-%s" }
                }
                """.formatted(lotId, lotId);
        this.mockMvc.perform(post("/lot").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void lotState_returnsCurrentSnapshot() throws Exception {
        postLot("LOT-Q1");

        this.mockMvc.perform(get("/lot/{id}", "LOT-Q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotId").value("LOT-Q1"))
                .andExpect(jsonPath("$.lotSpec").value("SP-Q"))
                .andExpect(jsonPath("$.statTyp").value("NEW"))
                .andExpect(jsonPath("$.createTm").isNotEmpty());
    }

    @Test
    void lotHistory_returnsAllStepsNewestFirst() throws Exception {
        postLot("LOT-Q2");

        this.mockMvc.perform(get("/lot/{id}/history", "LOT-Q2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].method").value("changeSpec"))
                .andExpect(jsonPath("$.content[1].method").value("created"))
                .andExpect(jsonPath("$.content[0].correlationId").value("TX-LOT-Q2"))
                .andExpect(jsonPath("$.content[0].timekey").isNotEmpty());
    }

    @Test
    void unknownLot_returns404() throws Exception {
        this.mockMvc.perform(get("/lot/{id}", "NOPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void wfAndDurableState_areQueryable() throws Exception {
        // WF
        String wf = """
                {
                  "entityType": "WF", "wfId": "WF-Q1", "lotId": "LOT-X",
                  "workflow": [ { "method": "created", "options": {}, "event": { "eventCd": "CREATED", "statTyp": "NEW" } } ],
                  "meta": { "srcSystem": "UI" }
                }
                """;
        this.mockMvc.perform(post("/wf").contentType(MediaType.APPLICATION_JSON).content(wf))
                .andExpect(status().isOk());
        this.mockMvc.perform(get("/wf/{id}", "WF-Q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wfId").value("WF-Q1"))
                .andExpect(jsonPath("$.lotId").value("LOT-X"));

        // DURABLE
        String dur = """
                {
                  "entityType": "DURABLE", "durableId": "DUR-Q1",
                  "workflow": [ { "method": "created", "options": {}, "event": { "eventCd": "CREATED", "statTyp": "NEW" } } ],
                  "meta": { "srcSystem": "UI" }
                }
                """;
        this.mockMvc.perform(post("/durable").contentType(MediaType.APPLICATION_JSON).content(dur))
                .andExpect(status().isOk());
        this.mockMvc.perform(get("/durable/{id}", "DUR-Q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durableId").value("DUR-Q1"));
        this.mockMvc.perform(get("/durable/{id}/history", "DUR-Q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void lotHistory_supportsPagingAndMethodFilter() throws Exception {
        postLot("LOT-Q3");

        // 페이징: size=1 → 1건, 전체 2건, 2페이지
        this.mockMvc.perform(get("/lot/{id}/history", "LOT-Q3")
                        .param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].method").value("changeSpec"));

        // 필터: method=created → 1건
        this.mockMvc.perform(get("/lot/{id}/history", "LOT-Q3")
                        .param("method", "created"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].method").value("created"));
    }
}
