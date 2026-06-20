package com.playtogether.pes.durable.collaboration;

import com.playtogether.pes.common.collaboration.CollaborationException;
import com.playtogether.pes.common.collaboration.DurableCollaborator;
import com.playtogether.pes.common.model.PesEventInfo;
import com.playtogether.pes.common.model.PesMeta;
import com.playtogether.pes.durable.api.DurableMessage;
import com.playtogether.pes.durable.api.DurableStepOptions;
import com.playtogether.pes.durable.api.DurableWorkflowStep;
import com.playtogether.pes.durable.service.DurableMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * DURABLE 협력자 구현. LOT 의 createDurable / makeDurableInUse 요청을 DurableMessageService 로 처리한다.
 * 서비스를 거치므로 파생 DURABLE 도 자기 이벤트(PES.UI.DURABLE.EVENT)를 발행한다.
 */
@Component
@RequiredArgsConstructor
public class DurableCollaboratorImpl implements DurableCollaborator {

    private final DurableMessageService messageService;

    @Override
    public void createDurableForLot(String durableId, String lotId, PesMeta meta) {
        requireDurableId(durableId, "createDurable", lotId);

        DurableWorkflowStep step = new DurableWorkflowStep(
                "created",
                DurableStepOptions.empty(),
                new PesEventInfo("CREATED", null, "DURABLE derived from LOT " + lotId, "NEW"));
        DurableMessage message = new DurableMessage("DURABLE", durableId, lotId, List.of(step), meta);

        this.messageService.handle(message);
    }

    @Override
    public void makeDurableInUse(String durableId, String lotId, PesMeta meta) {
        requireDurableId(durableId, "makeDurableInUse", lotId);

        DurableWorkflowStep step = new DurableWorkflowStep(
                "makeInUse",
                new DurableStepOptions(Map.of("bindLot", Boolean.TRUE)),
                new PesEventInfo("INUSE", null, "DURABLE InUse from LOT " + lotId, "IN_USE"));
        DurableMessage message = new DurableMessage("DURABLE", durableId, lotId, List.of(step), meta);

        this.messageService.handle(message);
    }

    private void requireDurableId(String durableId, String option, String lotId) {
        if (durableId == null || durableId.isBlank()) {
            throw new CollaborationException(
                    option + " 옵션 처리에는 durableId 가 필요합니다 (lotId=" + lotId + ")");
        }
    }
}
