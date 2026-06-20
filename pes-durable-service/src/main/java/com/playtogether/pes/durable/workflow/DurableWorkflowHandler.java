package com.playtogether.pes.durable.workflow;

import com.playtogether.pes.common.workflow.PesProcessStatus;
import com.playtogether.pes.common.workflow.PesStepResult;
import com.playtogether.pes.durable.api.DurableMessage;
import com.playtogether.pes.durable.api.DurableMethod;
import com.playtogether.pes.durable.api.DurableWorkflowStep;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DURABLE 메시지의 workflow 배열을 순차 처리한다. (LOT/WF 와 동일 패턴: 실패 시 이후 SKIPPED)
 */
@Component
public class DurableWorkflowHandler {

    private final Map<DurableMethod, DurableMethodHandler> handlers;

    public DurableWorkflowHandler(List<DurableMethodHandler> handlerBeans) {
        Map<DurableMethod, DurableMethodHandler> map = new EnumMap<>(DurableMethod.class);
        for (DurableMethodHandler handler : handlerBeans) {
            DurableMethodHandler previous = map.put(handler.method(), handler);
            if (previous != null) {
                throw new IllegalStateException(
                        "동일 method 에 핸들러가 중복 등록되었습니다: " + handler.method());
            }
        }
        this.handlers = Collections.unmodifiableMap(map);
    }

    public List<PesStepResult> process(DurableMessage message) {
        List<PesStepResult> results = new ArrayList<>(message.workflow().size());
        boolean aborted = false;

        for (DurableWorkflowStep step : message.workflow()) {
            if (aborted) {
                results.add(PesStepResult.skipped(step.method()));
                continue;
            }

            Optional<DurableMethod> resolved = step.resolvedMethod();
            if (resolved.isEmpty()) {
                results.add(PesStepResult.failed(step.method(), "지원하지 않는 method"));
                aborted = true;
                continue;
            }

            DurableMethodHandler handler = this.handlers.get(resolved.get());
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
