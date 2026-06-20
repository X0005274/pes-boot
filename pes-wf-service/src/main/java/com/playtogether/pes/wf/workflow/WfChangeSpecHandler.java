package com.playtogether.pes.wf.workflow;

import com.playtogether.pes.common.collaboration.LotCollaborator;
import com.playtogether.pes.common.support.PesTimekeyGenerator;
import com.playtogether.pes.common.workflow.PesStepResult;
import com.playtogether.pes.wf.api.WfMessage;
import com.playtogether.pes.wf.api.WfMethod;
import com.playtogether.pes.wf.api.WfWorkflowStep;
import com.playtogether.pes.wf.entity.PesWfMas;
import com.playtogether.pes.wf.repository.PesWfHisRepository;
import com.playtogether.pes.wf.repository.PesWfMasRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * WF 'changeSpec' 처리. PES_WF_MAS 의 spec 갱신 + PES_WF_HIS 적재.
 * 새 spec 값은 options.values["wfSpec"]. applyToLot 옵션 시 LOT 협력자로 전파한다.
 */
@Component
public class WfChangeSpecHandler extends AbstractWfMethodHandler {

    private static final String OPTION_WF_SPEC = "wfSpec";

    private final ObjectProvider<LotCollaborator> lotCollaborator;

    public WfChangeSpecHandler(PesWfMasRepository masRepository,
                               PesWfHisRepository hisRepository,
                               PesTimekeyGenerator timekeyGenerator,
                               ObjectProvider<LotCollaborator> lotCollaborator) {
        super(masRepository, hisRepository, timekeyGenerator);
        this.lotCollaborator = lotCollaborator;
    }

    @Override
    public WfMethod method() {
        return WfMethod.CHANGE_SPEC;
    }

    @Override
    public PesStepResult handle(WfMessage message, WfWorkflowStep step) {
        Optional<PesWfMas> found = this.masRepository.findById(message.wfId());
        if (found.isEmpty()) {
            return PesStepResult.failed(method().wireName(),
                    "존재하지 않는 WF: " + message.wfId());
        }

        Object newSpec = step.options().get(OPTION_WF_SPEC);
        if (newSpec == null) {
            return PesStepResult.failed(method().wireName(),
                    "changeSpec 옵션에 'wfSpec' 값이 없습니다: wfId=" + message.wfId());
        }

        PesWfMas mas = found.get();
        mas.setWfSpec(newSpec.toString());
        if (step.event() != null && step.event().statTyp() != null) {
            mas.setStatTyp(step.event().statTyp());
        }
        mas.setUpdateTm(this.timekeyGenerator.currentTimestamp());
        this.masRepository.save(mas);

        appendHistory(message, step);

        // 크로스 도메인: applyToLot -> 변경 spec 을 LOT(lotId) 에 전파
        if (step.options().applyToLot() && message.lotId() != null) {
            LotCollaborator lot = this.lotCollaborator.getIfAvailable();
            if (lot == null) {
                return PesStepResult.failed(method().wireName(),
                        "applyToLot 요청이나 LOT 협력자가 등록되지 않았습니다");
            }
            lot.applySpecToLot(message.lotId(), newSpec.toString(), message.meta());
        }

        // TODO: step.options().syncToHub() -> Hub 동기화
        return PesStepResult.success(method().wireName(),
                "WF changeSpec: wfId=" + message.wfId() + ", spec=" + newSpec);
    }
}
