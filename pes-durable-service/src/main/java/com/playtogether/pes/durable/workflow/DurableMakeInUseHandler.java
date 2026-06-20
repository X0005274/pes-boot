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

import java.util.Optional;

/**
 * DURABLE 'makeInUse' 처리. 상태를 InUse 로 전이 + (bindLot 시) LOT 바인딩 + PES_DURABLE_HIS 적재.
 */
@Component
public class DurableMakeInUseHandler extends AbstractDurableMethodHandler {

    public DurableMakeInUseHandler(PesDurableMasRepository masRepository,
                                   PesDurableHisRepository hisRepository,
                                   PesTimekeyGenerator timekeyGenerator) {
        super(masRepository, hisRepository, timekeyGenerator);
    }

    @Override
    public DurableMethod method() {
        return DurableMethod.MAKE_IN_USE;
    }

    @Override
    public PesStepResult handle(DurableMessage message, DurableWorkflowStep step) {
        Optional<PesDurableMas> found = this.masRepository.findById(message.durableId());
        if (found.isEmpty()) {
            return PesStepResult.failed(method().wireName(),
                    "존재하지 않는 DURABLE: " + message.durableId());
        }

        PesDurableMas mas = found.get();
        if (step.event() != null && step.event().statTyp() != null) {
            mas.setStatTyp(step.event().statTyp());
        }
        // bindLot: 메시지의 lotId 를 이 DURABLE 에 바인딩
        if (step.options().bindLot() && message.lotId() != null) {
            mas.setLotId(message.lotId());
        }
        mas.setUpdateTm(this.timekeyGenerator.currentTimestamp());
        this.masRepository.save(mas);

        appendHistory(message, step);

        // TODO: step.options().syncToEquipment() -> 설비 동기화
        // TODO: step.options().syncToHub()        -> Hub 동기화
        return PesStepResult.success(method().wireName(),
                "DURABLE makeInUse: durableId=" + message.durableId()
                        + ", lotId=" + message.lotId());
    }
}
