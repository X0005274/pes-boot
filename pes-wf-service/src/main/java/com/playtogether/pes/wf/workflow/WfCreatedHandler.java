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

/**
 * WF 'created' 처리. PES_WF_MAS 신규 생성 + PES_WF_HIS 적재.
 * inheritLotSpec 옵션 시 LOT 협력자로 LOT spec 을 조회해 WF_SPEC 에 반영한다.
 */
@Component
public class WfCreatedHandler extends AbstractWfMethodHandler {

    private final ObjectProvider<LotCollaborator> lotCollaborator;

    public WfCreatedHandler(PesWfMasRepository masRepository,
                            PesWfHisRepository hisRepository,
                            PesTimekeyGenerator timekeyGenerator,
                            ObjectProvider<LotCollaborator> lotCollaborator) {
        super(masRepository, hisRepository, timekeyGenerator);
        this.lotCollaborator = lotCollaborator;
    }

    @Override
    public WfMethod method() {
        return WfMethod.CREATED;
    }

    @Override
    public PesStepResult handle(WfMessage message, WfWorkflowStep step) {
        if (this.masRepository.existsById(message.wfId())) {
            return PesStepResult.failed(method().wireName(),
                    "이미 존재하는 WF: " + message.wfId());
        }

        String now = this.timekeyGenerator.currentTimestamp();
        PesWfMas mas = new PesWfMas(message.wfId());
        mas.setLotId(message.lotId());
        mas.setStatTyp(statTypOf(step));
        mas.setSrcSystem(message.meta().srcSystem());
        mas.setUserId(message.meta().userId());
        mas.setCreateTm(now);
        mas.setUpdateTm(now);

        // 크로스 도메인: inheritLotSpec -> LOT(lotId) 의 spec 을 WF_SPEC 에 상속
        if (step.options().inheritLotSpec() && message.lotId() != null) {
            LotCollaborator lot = this.lotCollaborator.getIfAvailable();
            if (lot != null) {
                lot.findLotSpec(message.lotId()).ifPresent(mas::setWfSpec);
            }
        }

        this.masRepository.save(mas);

        appendHistory(message, step);

        // TODO: step.options().syncToHub() -> Hub 동기화
        return PesStepResult.success(method().wireName(),
                "WF created: wfId=" + message.wfId() + ", lotId=" + message.lotId());
    }

    private String statTypOf(WfWorkflowStep step) {
        if (step.event() == null) {
            return null;
        }
        return step.event().statTyp();
    }
}
