package com.playtogether.pes.lot.workflow;

import com.playtogether.pes.common.collaboration.DurableCollaborator;
import com.playtogether.pes.common.collaboration.WfCollaborator;
import com.playtogether.pes.common.support.PesTimekeyGenerator;
import com.playtogether.pes.common.workflow.PesStepResult;
import com.playtogether.pes.lot.api.LotMessage;
import com.playtogether.pes.lot.api.LotMethod;
import com.playtogether.pes.lot.api.LotWorkflowStep;
import com.playtogether.pes.lot.entity.PesLotMas;
import com.playtogether.pes.lot.repository.PesLotHisRepository;
import com.playtogether.pes.lot.repository.PesLotMasRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * LOT 'created' 처리. PES_LOT_MAS 신규 생성 + PES_LOT_HIS 적재.
 * createWf / createDurable 옵션 시 WF/DURABLE 협력자에 파생 생성을 위임한다.
 */
@Component
public class LotCreatedHandler extends AbstractLotMethodHandler {

    private final ObjectProvider<WfCollaborator> wfCollaborator;
    private final ObjectProvider<DurableCollaborator> durableCollaborator;

    public LotCreatedHandler(PesLotMasRepository masRepository,
                             PesLotHisRepository hisRepository,
                             PesTimekeyGenerator timekeyGenerator,
                             ObjectProvider<WfCollaborator> wfCollaborator,
                             ObjectProvider<DurableCollaborator> durableCollaborator) {
        super(masRepository, hisRepository, timekeyGenerator);
        this.wfCollaborator = wfCollaborator;
        this.durableCollaborator = durableCollaborator;
    }

    @Override
    public LotMethod method() {
        return LotMethod.CREATED;
    }

    @Override
    public PesStepResult handle(LotMessage message, LotWorkflowStep step) {
        if (this.masRepository.existsById(message.lotId())) {
            return PesStepResult.failed(method().wireName(),
                    "이미 존재하는 LOT: " + message.lotId());
        }

        String now = this.timekeyGenerator.currentTimestamp();
        PesLotMas mas = new PesLotMas(message.lotId());
        mas.setWfId(message.wfId());
        mas.setDurableId(message.durableId());
        mas.setStatTyp(statTypOf(step));
        mas.setSrcSystem(message.meta().srcSystem());
        mas.setUserId(message.meta().userId());
        mas.setCreateTm(now);
        mas.setUpdateTm(now);
        this.masRepository.save(mas);

        appendHistory(message, step);

        // 크로스 도메인: createWf -> WF 파생 생성
        if (step.options().createWf()) {
            WfCollaborator wf = this.wfCollaborator.getIfAvailable();
            if (wf == null) {
                return PesStepResult.failed(method().wireName(),
                        "createWf 요청이나 WF 협력자가 등록되지 않았습니다");
            }
            wf.createWfForLot(message.wfId(), message.lotId(), message.meta());
        }

        // 크로스 도메인: createDurable -> DURABLE 파생 생성
        if (step.options().createDurable()) {
            DurableCollaborator durable = this.durableCollaborator.getIfAvailable();
            if (durable == null) {
                return PesStepResult.failed(method().wireName(),
                        "createDurable 요청이나 DURABLE 협력자가 등록되지 않았습니다");
            }
            durable.createDurableForLot(message.durableId(), message.lotId(), message.meta());
        }

        // TODO: step.options().syncToHub() -> Hub 동기화
        return PesStepResult.success(method().wireName(),
                "LOT created: lotId=" + message.lotId());
    }

    private String statTypOf(LotWorkflowStep step) {
        if (step.event() == null) {
            return null;
        }
        return step.event().statTyp();
    }
}
