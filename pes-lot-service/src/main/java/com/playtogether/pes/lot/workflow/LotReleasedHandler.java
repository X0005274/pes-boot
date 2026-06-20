package com.playtogether.pes.lot.workflow;

import com.playtogether.pes.common.collaboration.DurableCollaborator;
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

import java.util.Optional;

/**
 * LOT 'released' 처리. PES_LOT_MAS 상태 갱신 + PES_LOT_HIS 적재.
 * makeDurableInUse 옵션 시 DURABLE 협력자에 InUse 전이를 위임한다.
 */
@Component
public class LotReleasedHandler extends AbstractLotMethodHandler {

    private final ObjectProvider<DurableCollaborator> durableCollaborator;

    public LotReleasedHandler(PesLotMasRepository masRepository,
                              PesLotHisRepository hisRepository,
                              PesTimekeyGenerator timekeyGenerator,
                              ObjectProvider<DurableCollaborator> durableCollaborator) {
        super(masRepository, hisRepository, timekeyGenerator);
        this.durableCollaborator = durableCollaborator;
    }

    @Override
    public LotMethod method() {
        return LotMethod.RELEASED;
    }

    @Override
    public PesStepResult handle(LotMessage message, LotWorkflowStep step) {
        Optional<PesLotMas> found = this.masRepository.findById(message.lotId());
        if (found.isEmpty()) {
            return PesStepResult.failed(method().wireName(),
                    "존재하지 않는 LOT: " + message.lotId());
        }

        PesLotMas mas = found.get();
        if (step.event() != null && step.event().statTyp() != null) {
            mas.setStatTyp(step.event().statTyp());
        }
        if (message.durableId() != null) {
            mas.setDurableId(message.durableId());
        }
        mas.setUpdateTm(this.timekeyGenerator.currentTimestamp());
        this.masRepository.save(mas);

        appendHistory(message, step);

        // 크로스 도메인: makeDurableInUse -> DURABLE InUse 전이 + LOT 바인딩
        if (step.options().makeDurableInUse() && message.durableId() != null) {
            DurableCollaborator durable = this.durableCollaborator.getIfAvailable();
            if (durable == null) {
                return PesStepResult.failed(method().wireName(),
                        "makeDurableInUse 요청이나 DURABLE 협력자가 등록되지 않았습니다");
            }
            durable.makeDurableInUse(message.durableId(), message.lotId(), message.meta());
        }

        // TODO: step.options().syncToHub() -> Hub 동기화
        return PesStepResult.success(method().wireName(),
                "LOT released: lotId=" + message.lotId()
                        + ", durableId=" + message.durableId());
    }
}
