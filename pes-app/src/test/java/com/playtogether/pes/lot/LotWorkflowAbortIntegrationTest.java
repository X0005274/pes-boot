package com.playtogether.pes.lot;

import com.playtogether.pes.common.model.PesEventInfo;
import com.playtogether.pes.common.model.PesMeta;
import com.playtogether.pes.common.workflow.PesProcessResult;
import com.playtogether.pes.common.workflow.PesProcessRollbackException;
import com.playtogether.pes.common.workflow.PesProcessStatus;
import com.playtogether.pes.lot.api.LotMessage;
import com.playtogether.pes.lot.api.LotStepOptions;
import com.playtogether.pes.lot.api.LotWorkflowStep;
import com.playtogether.pes.lot.repository.PesLotHisRepository;
import com.playtogether.pes.lot.service.LotMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * 워크플로우 순차 처리의 실패-중단(abort) + 롤백 의미 검증.
 * 첫 step 이 FAILED 면 이후 step 은 SKIPPED 로 기록되고, 전체 트랜잭션이 롤백된다(예외 전파).
 */
@SpringBootTest
class LotWorkflowAbortIntegrationTest {

    @Autowired
    private LotMessageService lotMessageService;

    @Autowired
    private PesLotHisRepository hisRepository;

    @Test
    void firstStepFails_subsequentStepsAreSkipped_andRollsBack() {
        String lotId = "LOT-ABORT-1";
        // 존재하지 않는 LOT 에 released → 실패, 이후 created 는 SKIPPED 되어야 함
        LotWorkflowStep releasedStep = new LotWorkflowStep(
                "released",
                LotStepOptions.empty(),
                new PesEventInfo("RELEASED", null, null, "REL"));
        LotWorkflowStep createdStep = new LotWorkflowStep(
                "created",
                LotStepOptions.empty(),
                new PesEventInfo("CREATED", null, null, "NEW"));

        LotMessage message = new LotMessage(
                "LOT", lotId, null, null,
                List.of(releasedStep, createdStep),
                new PesMeta("UI", null, null, null, null));

        // 실패 → 롤백 예외 전파(구조화된 결과 운반)
        PesProcessRollbackException ex = catchThrowableOfType(
                () -> this.lotMessageService.handle(message),
                PesProcessRollbackException.class);

        assertThat(ex).isNotNull();
        PesProcessResult result = ex.getResult();
        assertThat(result.status()).isEqualTo(PesProcessStatus.FAILED);
        assertThat(result.steps()).hasSize(2);
        assertThat(result.steps().get(0).status()).isEqualTo(PesProcessStatus.FAILED);
        assertThat(result.steps().get(0).method()).isEqualTo("released");
        assertThat(result.steps().get(1).status()).isEqualTo(PesProcessStatus.SKIPPED);
        assertThat(result.steps().get(1).method()).isEqualTo("created");

        // 롤백되어 HIS 미적재
        assertThat(this.hisRepository.findByIdLotIdOrderByIdTimekeyDesc(lotId)).isEmpty();
    }

    @Test
    void changeSpecWithoutLotSpec_throwsRollback() {
        // 단일 도메인 실패도 롤백 예외로 전파됨을 확인
        LotWorkflowStep step = new LotWorkflowStep(
                "changeSpec",
                LotStepOptions.empty(),
                new PesEventInfo("CHG", null, null, "NEW"));
        LotMessage message = new LotMessage(
                "LOT", "LOT-ABORT-2", null, null,
                List.of(step),
                new PesMeta("UI", null, null, null, null));

        assertThatThrownBy(() -> this.lotMessageService.handle(message))
                .isInstanceOf(PesProcessRollbackException.class);
    }
}
