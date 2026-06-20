package com.playtogether.pes.wf.service;

import com.playtogether.pes.common.messaging.PesEventSink;
import com.playtogether.pes.common.model.EntityType;
import com.playtogether.pes.common.workflow.PesDomainHandler;
import com.playtogether.pes.common.workflow.PesProcessResult;
import com.playtogether.pes.common.workflow.PesProcessRollbackException;
import com.playtogether.pes.common.workflow.PesProcessStatus;
import com.playtogether.pes.common.workflow.PesStepResult;
import com.playtogether.pes.wf.api.WfMessage;
import com.playtogether.pes.wf.workflow.WfWorkflowHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * WF 도메인 진입점. PesDomainRouter 가 EntityType.WF 메시지를 이 빈으로 라우팅한다.
 */
@Service
@RequiredArgsConstructor
public class WfMessageService implements PesDomainHandler<WfMessage> {

    private final WfWorkflowHandler workflowHandler;
    private final ObjectProvider<PesEventSink> eventSink;

    @Override
    public EntityType entityType() {
        return EntityType.WF;
    }

    @Override
    @Transactional
    public PesProcessResult handle(WfMessage message) {
        message.assertContract();

        List<PesStepResult> steps = this.workflowHandler.process(message);
        PesProcessStatus overall = resolveOverall(steps);

        PesProcessResult result = new PesProcessResult(
                message.entityType(),
                message.wfId(),
                message.meta() != null ? message.meta().correlationId() : null,
                overall,
                steps);

        if (overall != PesProcessStatus.SUCCESS) {
            throw new PesProcessRollbackException(result);
        }

        PesEventSink sink = this.eventSink.getIfAvailable();
        if (sink != null) {
            sink.publishResult(EntityType.WF, result);
        }
        return result;
    }

    private PesProcessStatus resolveOverall(List<PesStepResult> steps) {
        for (PesStepResult step : steps) {
            if (step.status() == PesProcessStatus.FAILED) {
                return PesProcessStatus.FAILED;
            }
        }
        return PesProcessStatus.SUCCESS;
    }
}
