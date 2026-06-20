package com.playtogether.pes.lot.collaboration;

import com.playtogether.pes.common.collaboration.LotCollaborator;
import com.playtogether.pes.common.model.PesEventInfo;
import com.playtogether.pes.common.model.PesMeta;
import com.playtogether.pes.lot.api.LotMessage;
import com.playtogether.pes.lot.api.LotStepOptions;
import com.playtogether.pes.lot.api.LotWorkflowStep;
import com.playtogether.pes.lot.entity.PesLotMas;
import com.playtogether.pes.lot.repository.PesLotMasRepository;
import com.playtogether.pes.lot.service.LotMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * LOT 협력자 구현. WF 의 inheritLotSpec(조회) / applyToLot(전파) 요청을 처리한다.
 * applyToLot 은 LotMessageService 를 거치므로 LOT 이벤트(PES.UI.LOT.EVENT)도 발행된다.
 */
@Component
@RequiredArgsConstructor
public class LotCollaboratorImpl implements LotCollaborator {

    private final PesLotMasRepository masRepository;
    private final LotMessageService messageService;

    @Override
    public Optional<String> findLotSpec(String lotId) {
        if (lotId == null) {
            return Optional.empty();
        }
        return this.masRepository.findById(lotId)
                .map(PesLotMas::getLotSpec)
                .filter(spec -> spec != null && !spec.isBlank());
    }

    @Override
    public void applySpecToLot(String lotId, String spec, PesMeta meta) {
        LotWorkflowStep step = new LotWorkflowStep(
                "changeSpec",
                new LotStepOptions(Map.of("lotSpec", spec)),
                new PesEventInfo("CHG", null, "spec applied from WF", "NEW"));
        LotMessage message = new LotMessage("LOT", lotId, null, null, List.of(step), meta);

        this.messageService.handle(message);
    }
}
