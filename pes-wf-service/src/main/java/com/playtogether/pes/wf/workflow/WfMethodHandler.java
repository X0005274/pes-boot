package com.playtogether.pes.wf.workflow;

import com.playtogether.pes.common.workflow.PesStepResult;
import com.playtogether.pes.wf.api.WfMessage;
import com.playtogether.pes.wf.api.WfMethod;
import com.playtogether.pes.wf.api.WfWorkflowStep;

/**
 * WF method 단위 처리 전략. (LOT 과 동일 패턴)
 */
public interface WfMethodHandler {

    WfMethod method();

    PesStepResult handle(WfMessage message, WfWorkflowStep step);
}
