package com.playtogether.pes.durable.workflow;

import com.playtogether.pes.common.workflow.PesStepResult;
import com.playtogether.pes.durable.api.DurableMessage;
import com.playtogether.pes.durable.api.DurableMethod;
import com.playtogether.pes.durable.api.DurableWorkflowStep;

/**
 * DURABLE method 단위 처리 전략. (LOT/WF 와 동일 패턴)
 */
public interface DurableMethodHandler {

    DurableMethod method();

    PesStepResult handle(DurableMessage message, DurableWorkflowStep step);
}
