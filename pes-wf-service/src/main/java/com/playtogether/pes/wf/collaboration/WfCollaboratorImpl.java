package com.playtogether.pes.wf.collaboration;

import com.playtogether.pes.common.collaboration.CollaborationException;
import com.playtogether.pes.common.collaboration.WfCollaborator;
import com.playtogether.pes.common.model.PesEventInfo;
import com.playtogether.pes.common.model.PesMeta;
import com.playtogether.pes.wf.api.WfMessage;
import com.playtogether.pes.wf.api.WfStepOptions;
import com.playtogether.pes.wf.api.WfWorkflowStep;
import com.playtogether.pes.wf.service.WfMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WF 협력자 구현. LOT 등 타 도메인의 파생 WF 생성 요청을 WfMessageService 로 처리한다.
 * 서비스를 거치므로 파생 WF 도 자기 이벤트(PES.UI.WF.EVENT)를 발행한다.
 * 호출 도메인의 트랜잭션에 그대로 참여한다.
 */
@Component
@RequiredArgsConstructor
public class WfCollaboratorImpl implements WfCollaborator {

    private final WfMessageService messageService;

    @Override
    public void createWfForLot(String wfId, String lotId, PesMeta meta) {
        if (wfId == null || wfId.isBlank()) {
            throw new CollaborationException(
                    "createWf 옵션 처리에는 wfId 가 필요합니다 (lotId=" + lotId + ")");
        }

        WfWorkflowStep step = new WfWorkflowStep(
                "created",
                WfStepOptions.empty(),
                new PesEventInfo("CREATED", null, "WF derived from LOT " + lotId, "NEW"));
        WfMessage message = new WfMessage("WF", wfId, lotId, List.of(step), meta);

        // 실패 시 WfMessageService 가 PesProcessRollbackException 을 던져 호출 트랜잭션 전체가 롤백된다.
        this.messageService.handle(message);
    }
}
