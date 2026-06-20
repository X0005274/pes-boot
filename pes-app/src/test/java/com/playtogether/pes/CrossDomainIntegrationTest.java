package com.playtogether.pes;

import com.playtogether.pes.durable.entity.PesDurableMas;
import com.playtogether.pes.durable.repository.PesDurableMasRepository;
import com.playtogether.pes.lot.entity.PesLotMas;
import com.playtogether.pes.lot.repository.PesLotMasRepository;
import com.playtogether.pes.wf.entity.PesWfMas;
import com.playtogether.pes.wf.repository.PesWfMasRepository;
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
 * 크로스 도메인 연계 통합 테스트. 한 도메인의 옵션이 타 도메인 협력자를 통해 실제로 처리되는지 검증.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CrossDomainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PesLotMasRepository lotMasRepository;

    @Autowired
    private PesWfMasRepository wfMasRepository;

    @Autowired
    private PesDurableMasRepository durableMasRepository;

    @Test
    void lotCreated_withCreateWfAndCreateDurable_derivesBothDomains() throws Exception {
        String lotId = "LOT-CD-1";
        String wfId = "WF-CD-1";
        String durableId = "DUR-CD-1";
        String body = """
                {
                  "entityType": "LOT",
                  "lotId": "%s",
                  "wfId": "%s",
                  "durableId": "%s",
                  "workflow": [
                    {
                      "method": "created",
                      "options": { "createWf": true, "createDurable": true },
                      "event": { "eventCd": "CREATED", "statTyp": "NEW" }
                    }
                  ],
                  "meta": { "srcSystem": "UI", "userId": "U1" }
                }
                """.formatted(lotId, wfId, durableId);

        this.mockMvc.perform(post("/lot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // LOT 생성 + WF/DURABLE 파생 생성이 같은 트랜잭션에서 커밋됨
        assertThat(this.lotMasRepository.findById(lotId)).isPresent();

        Optional<PesWfMas> wf = this.wfMasRepository.findById(wfId);
        assertThat(wf).isPresent();
        assertThat(wf.get().getLotId()).isEqualTo(lotId);

        Optional<PesDurableMas> durable = this.durableMasRepository.findById(durableId);
        assertThat(durable).isPresent();
        assertThat(durable.get().getLotId()).isEqualTo(lotId);
    }

    @Test
    void lotReleased_withMakeDurableInUse_transitionsDurable() throws Exception {
        String lotId = "LOT-CD-2";
        String durableId = "DUR-CD-2";
        // created(createDurable) -> released(makeDurableInUse)
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
                      "method": "released",
                      "options": { "makeDurableInUse": true },
                      "event": { "eventCd": "RELEASED", "statTyp": "REL" }
                    }
                  ],
                  "meta": { "srcSystem": "UI" }
                }
                """.formatted(lotId, durableId);

        this.mockMvc.perform(post("/lot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        Optional<PesDurableMas> durable = this.durableMasRepository.findById(durableId);
        assertThat(durable).isPresent();
        assertThat(durable.get().getStatTyp()).isEqualTo("IN_USE");
        assertThat(durable.get().getLotId()).isEqualTo(lotId);
    }

    @Test
    void wfCreated_withInheritLotSpec_copiesLotSpecToWf() throws Exception {
        String lotId = "LOT-CD-3";
        String wfId = "WF-CD-3";
        // 1) LOT 생성 + spec 설정
        String lotBody = """
                {
                  "entityType": "LOT",
                  "lotId": "%s",
                  "workflow": [
                    { "method": "created", "options": {}, "event": { "eventCd": "CREATED", "statTyp": "NEW" } },
                    { "method": "changeSpec", "options": { "lotSpec": "SPEC-INH" }, "event": { "eventCd": "CHG", "statTyp": "NEW" } }
                  ],
                  "meta": { "srcSystem": "UI" }
                }
                """.formatted(lotId);
        this.mockMvc.perform(post("/lot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lotBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // 2) WF 생성 + inheritLotSpec
        String wfBody = """
                {
                  "entityType": "WF",
                  "wfId": "%s",
                  "lotId": "%s",
                  "workflow": [
                    { "method": "created", "options": { "inheritLotSpec": true }, "event": { "eventCd": "CREATED", "statTyp": "NEW" } }
                  ],
                  "meta": { "srcSystem": "UI" }
                }
                """.formatted(wfId, lotId);
        this.mockMvc.perform(post("/wf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wfBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        Optional<PesWfMas> wf = this.wfMasRepository.findById(wfId);
        assertThat(wf).isPresent();
        assertThat(wf.get().getWfSpec()).isEqualTo("SPEC-INH");
    }

    @Test
    void wfChangeSpec_withApplyToLot_propagatesSpecToLot() throws Exception {
        String lotId = "LOT-CD-4";
        String wfId = "WF-CD-4";
        // LOT, WF 선행 생성
        String lotBody = """
                {
                  "entityType": "LOT",
                  "lotId": "%s",
                  "workflow": [ { "method": "created", "options": {}, "event": { "eventCd": "CREATED", "statTyp": "NEW" } } ],
                  "meta": { "srcSystem": "UI" }
                }
                """.formatted(lotId);
        this.mockMvc.perform(post("/lot").contentType(MediaType.APPLICATION_JSON).content(lotBody))
                .andExpect(status().isOk());

        String wfBody = """
                {
                  "entityType": "WF",
                  "wfId": "%s",
                  "lotId": "%s",
                  "workflow": [
                    { "method": "created", "options": {}, "event": { "eventCd": "CREATED", "statTyp": "NEW" } },
                    { "method": "changeSpec", "options": { "wfSpec": "SPEC-PROP", "applyToLot": true }, "event": { "eventCd": "CHG", "statTyp": "NEW" } }
                  ],
                  "meta": { "srcSystem": "UI" }
                }
                """.formatted(wfId, lotId);
        this.mockMvc.perform(post("/wf").contentType(MediaType.APPLICATION_JSON).content(wfBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        Optional<PesLotMas> lot = this.lotMasRepository.findById(lotId);
        assertThat(lot).isPresent();
        assertThat(lot.get().getLotSpec()).isEqualTo("SPEC-PROP");
    }
}
