package com.playtogether.pes.wf.workflow;

import com.playtogether.pes.common.support.PesTimekeyGenerator;
import com.playtogether.pes.common.workflow.PesStepResult;
import com.playtogether.pes.wf.api.WfMessage;
import com.playtogether.pes.wf.api.WfMethod;
import com.playtogether.pes.wf.api.WfWorkflowStep;
import com.playtogether.pes.wf.entity.PesWfMas;
import com.playtogether.pes.wf.repository.PesWfHisRepository;
import com.playtogether.pes.wf.repository.PesWfMasRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * WF 'released' 처리. PES_WF_MAS 상태 갱신 + PES_WF_HIS 적재.
 */
@Component
public class WfReleasedHandler extends AbstractWfMethodHandler {

    public WfReleasedHandler(PesWfMasRepository masRepository,
                             PesWfHisRepository hisRepository,
                             PesTimekeyGenerator timekeyGenerator) {
        super(masRepository, hisRepository, timekeyGenerator);
    }

    @Override
    public WfMethod method() {
        return WfMethod.RELEASED;
    }

    @Override
    public PesStepResult handle(WfMessage message, WfWorkflowStep step) {
        Optional<PesWfMas> found = this.masRepository.findById(message.wfId());
        if (found.isEmpty()) {
            return PesStepResult.failed(method().wireName(),
                    "존재하지 않는 WF: " + message.wfId());
        }

        PesWfMas mas = found.get();
        if (step.event() != null && step.event().statTyp() != null) {
            mas.setStatTyp(step.event().statTyp());
        }
        mas.setUpdateTm(this.timekeyGenerator.currentTimestamp());
        this.masRepository.save(mas);

        appendHistory(message, step);

        // TODO: step.options().syncToHub() -> Hub 동기화
        return PesStepResult.success(method().wireName(),
                "WF released: wfId=" + message.wfId());
    }
}
