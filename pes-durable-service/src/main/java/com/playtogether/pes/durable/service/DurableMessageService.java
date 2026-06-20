package com.playtogether.pes.durable.service;

import com.playtogether.pes.common.messaging.PesEventSink;
import com.playtogether.pes.common.model.EntityType;
import com.playtogether.pes.common.workflow.PesDomainHandler;
import com.playtogether.pes.common.workflow.PesProcessResult;
import com.playtogether.pes.common.workflow.PesProcessRollbackException;
import com.playtogether.pes.common.workflow.PesProcessStatus;
import com.playtogether.pes.common.workflow.PesStepResult;
import com.playtogether.pes.durable.api.DurableMessage;
import com.playtogether.pes.durable.workflow.DurableWorkflowHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DURABLE 도메인 진입점. PesDomainRouter 가 EntityType.DURABLE 메시지를 이 빈으로 라우팅한다.
 */
@Service
@RequiredArgsConstructor
public class DurableMessageService implements PesDomainHandler<DurableMessage> {

    private final DurableWorkflowHandler workflowHandler;
    private final ObjectProvider<PesEventSink> eventSink;

    @Override
    public EntityType entityType() {
        return EntityType.DURABLE;
    }

    @Override
    @Transactional
    public PesProcessResult handle(DurableMessage message) {
        message.assertContract();

        List<PesStepResult> steps = this.workflowHandler.process(message);
        PesProcessStatus overall = resolveOverall(steps);

        PesProcessResult result = new PesProcessResult(
                message.entityType(),
                message.durableId(),
                message.meta() != null ? message.meta().correlationId() : null,
                overall,
                steps);

        if (overall != PesProcessStatus.SUCCESS) {
            throw new PesProcessRollbackException(result);
        }

        PesEventSink sink = this.eventSink.getIfAvailable();
        if (sink != null) {
            sink.publishResult(EntityType.DURABLE, result);
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
