package com.playtogether.pes.durable.workflow;

import com.playtogether.pes.common.support.PesTimekeyGenerator;
import com.playtogether.pes.common.workflow.PesStepResult;
import com.playtogether.pes.durable.api.DurableMessage;
import com.playtogether.pes.durable.api.DurableMethod;
import com.playtogether.pes.durable.api.DurableWorkflowStep;
import com.playtogether.pes.durable.entity.PesDurableMas;
import com.playtogether.pes.durable.repository.PesDurableHisRepository;
import com.playtogether.pes.durable.repository.PesDurableMasRepository;
import org.springframework.stereotype.Component;

/**
 * DURABLE 'created' 처리. PES_DURABLE_MAS 신규 생성 + PES_DURABLE_HIS 적재.
 */
@Component
public class DurableCreatedHandler extends AbstractDurableMethodHandler {

    public DurableCreatedHandler(PesDurableMasRepository masRepository,
                                 PesDurableHisRepository hisRepository,
                                 PesTimekeyGenerator timekeyGenerator) {
        super(masRepository, hisRepository, timekeyGenerator);
    }

    @Override
    public DurableMethod method() {
        return DurableMethod.CREATED;
    }

    @Override
    public PesStepResult handle(DurableMessage message, DurableWorkflowStep step) {
        if (this.masRepository.existsById(message.durableId())) {
            return PesStepResult.failed(method().wireName(),
                    "이미 존재하는 DURABLE: " + message.durableId());
        }

        String now = this.timekeyGenerator.currentTimestamp();
        PesDurableMas mas = new PesDurableMas(message.durableId());
        mas.setLotId(message.lotId());
        mas.setStatTyp(statTypOf(step));
        mas.setSrcSystem(message.meta().srcSystem());
        mas.setUserId(message.meta().userId());
        mas.setCreateTm(now);
        mas.setUpdateTm(now);
        this.masRepository.save(mas);

        appendHistory(message, step);

        // TODO: step.options().syncToEquipment() -> 설비 동기화
        // TODO: step.options().syncToHub()        -> Hub 동기화
        return PesStepResult.success(method().wireName(),
                "DURABLE created: durableId=" + message.durableId());
    }

    private String statTypOf(DurableWorkflowStep step) {
        if (step.event() == null) {
            return null;
        }
        return step.event().statTyp();
    }
}
