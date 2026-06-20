package com.playtogether.pes.wf.workflow;

import com.playtogether.pes.common.workflow.PesProcessStatus;
import com.playtogether.pes.common.workflow.PesStepResult;
import com.playtogether.pes.wf.api.WfMessage;
import com.playtogether.pes.wf.api.WfMethod;
import com.playtogether.pes.wf.api.WfWorkflowStep;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * WF 메시지의 workflow 배열을 순차 처리한다. (LOT 과 동일 패턴: 실패 시 이후 SKIPPED)
 */
@Component
public class WfWorkflowHandler {

    private final Map<WfMethod, WfMethodHandler> handlers;

    public WfWorkflowHandler(List<WfMethodHandler> handlerBeans) {
        Map<WfMethod, WfMethodHandler> map = new EnumMap<>(WfMethod.class);
        for (WfMethodHandler handler : handlerBeans) {
            WfMethodHandler previous = map.put(handler.method(), handler);
            if (previous != null) {
                throw new IllegalStateException(
                        "동일 method 에 핸들러가 중복 등록되었습니다: " + handler.method());
            }
        }
        this.handlers = Collections.unmodifiableMap(map);
    }

    public List<PesStepResult> process(WfMessage message) {
        List<PesStepResult> results = new ArrayList<>(message.workflow().size());
        boolean aborted = false;

        for (WfWorkflowStep step : message.workflow()) {
            if (aborted) {
                results.add(PesStepResult.skipped(step.method()));
                continue;
            }

            Optional<WfMethod> resolved = step.resolvedMethod();
            if (resolved.isEmpty()) {
                results.add(PesStepResult.failed(step.method(), "지원하지 않는 method"));
                aborted = true;
                continue;
            }

            WfMethodHandler handler = this.handlers.get(resolved.get());
            if (handler == null) {
                results.add(PesStepResult.failed(step.method(), "등록된 핸들러 없음"));
                aborted = true;
                continue;
            }

            try {
                PesStepResult result = handler.handle(message, step);
                results.add(result);
                if (result.status() == PesProcessStatus.FAILED) {
                    aborted = true;
                }
            } catch (RuntimeException ex) {
                results.add(PesStepResult.failed(step.method(),
                        "처리 중 예외: " + ex.getMessage()));
                aborted = true;
            }
        }
        return results;
    }
}
