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
 * DURABLE 'changeSpec' 처리. PES_DURABLE_MAS 의 spec 갱신 + PES_DURABLE_HIS 적재.
 * 새 spec 값은 options.values["durableSpec"] 로 전달받는다.
 */
@Component
public class DurableChangeSpecHandler extends AbstractDurableMethodHandler {

    private static final String OPTION_DURABLE_SPEC = "durableSpec";

    public DurableChangeSpecHandler(PesDurableMasRepository masRepository,
                                    PesDurableHisRepository hisRepository,
                                    PesTimekeyGenerator timekeyGenerator) {
        super(masRepository, hisRepository, timekeyGenerator);
    }

    @Override
    public DurableMethod method() {
        return DurableMethod.CHANGE_SPEC;
    }

    @Override
    public PesStepResult handle(DurableMessage message, DurableWorkflowStep step) {
        Optional<PesDurableMas> found = this.masRepository.findById(message.durableId());
        if (found.isEmpty()) {
            return PesStepResult.failed(method().wireName(),
                    "존재하지 않는 DURABLE: " + message.durableId());
        }

        Object newSpec = step.options().get(OPTION_DURABLE_SPEC);
        if (newSpec == null) {
            return PesStepResult.failed(method().wireName(),
                    "changeSpec 옵션에 'durableSpec' 값이 없습니다: durableId=" + message.durableId());
        }

        PesDurableMas mas = found.get();
        mas.setDurableSpec(newSpec.toString());
        if (step.event() != null && step.event().statTyp() != null) {
            mas.setStatTyp(step.event().statTyp());
        }
        mas.setUpdateTm(this.timekeyGenerator.currentTimestamp());
        this.masRepository.save(mas);

        appendHistory(message, step);

        // TODO: step.options().syncToHub() -> Hub 동기화
        return PesStepResult.success(method().wireName(),
                "DURABLE changeSpec: durableId=" + message.durableId() + ", spec=" + newSpec);
    }
}
