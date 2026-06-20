package com.playtogether.pes.lot.workflow;

import com.playtogether.pes.common.support.PesTimekeyGenerator;
import com.playtogether.pes.common.workflow.PesStepResult;
import com.playtogether.pes.lot.api.LotMessage;
import com.playtogether.pes.lot.api.LotMethod;
import com.playtogether.pes.lot.api.LotWorkflowStep;
import com.playtogether.pes.lot.entity.PesLotMas;
import com.playtogether.pes.lot.repository.PesLotHisRepository;
import com.playtogether.pes.lot.repository.PesLotMasRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * LOT 'changeSpec' 처리. PES_LOT_MAS 의 spec 갱신 + PES_LOT_HIS 적재.
 * 새 spec 값은 options.extra["lotSpec"] 로 전달받는다(계약 외 옵션 흡수 영역).
 */
@Component
public class LotChangeSpecHandler extends AbstractLotMethodHandler {

    private static final String OPTION_LOT_SPEC = "lotSpec";

    public LotChangeSpecHandler(PesLotMasRepository masRepository,
                                PesLotHisRepository hisRepository,
                                PesTimekeyGenerator timekeyGenerator) {
        super(masRepository, hisRepository, timekeyGenerator);
    }

    @Override
    public LotMethod method() {
        return LotMethod.CHANGE_SPEC;
    }

    @Override
    public PesStepResult handle(LotMessage message, LotWorkflowStep step) {
        Optional<PesLotMas> found = this.masRepository.findById(message.lotId());
        if (found.isEmpty()) {
            return PesStepResult.failed(method().wireName(),
                    "존재하지 않는 LOT: " + message.lotId());
        }

        Object newSpec = step.options().get(OPTION_LOT_SPEC);
        if (newSpec == null) {
            return PesStepResult.failed(method().wireName(),
                    "changeSpec 옵션에 'lotSpec' 값이 없습니다: lotId=" + message.lotId());
        }

        PesLotMas mas = found.get();
        mas.setLotSpec(newSpec.toString());
        if (step.event() != null && step.event().statTyp() != null) {
            mas.setStatTyp(step.event().statTyp());
        }
        mas.setUpdateTm(this.timekeyGenerator.currentTimestamp());
        this.masRepository.save(mas);

        appendHistory(message, step);

        // TODO: step.options().syncToHub() -> Hub 동기화
        return PesStepResult.success(method().wireName(),
                "LOT changeSpec: lotId=" + message.lotId() + ", spec=" + newSpec);
    }
}
