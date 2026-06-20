package com.playtogether.pes.lot.workflow;

import com.playtogether.pes.common.workflow.PesProcessStatus;
import com.playtogether.pes.common.workflow.PesStepResult;
import com.playtogether.pes.lot.api.LotMessage;
import com.playtogether.pes.lot.api.LotMethod;
import com.playtogether.pes.lot.api.LotWorkflowStep;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * LOT 메시지의 workflow 배열을 <b>순차적으로</b> 처리한다.
 * 한 step 이 실패하면 이후 step 은 SKIPPED 로 표시하고 중단한다
 * (created -&gt; released 처럼 뒷 step 이 앞 step 결과에 의존하므로).
 */
@Component
public class LotWorkflowHandler {

    private final Map<LotMethod, LotMethodHandler> handlers;

    public LotWorkflowHandler(List<LotMethodHandler> handlerBeans) {
        Map<LotMethod, LotMethodHandler> map = new EnumMap<>(LotMethod.class);
        for (LotMethodHandler handler : handlerBeans) {
            LotMethodHandler previous = map.put(handler.method(), handler);
            if (previous != null) {
                throw new IllegalStateException(
                        "동일 method 에 핸들러가 중복 등록되었습니다: " + handler.method());
            }
        }
        this.handlers = Collections.unmodifiableMap(map);
    }

    public List<PesStepResult> process(LotMessage message) {
        List<PesStepResult> results = new ArrayList<>(message.workflow().size());
        boolean aborted = false;

        for (LotWorkflowStep step : message.workflow()) {
            if (aborted) {
                results.add(PesStepResult.skipped(step.method()));
                continue;
            }

            Optional<LotMethod> resolved = step.resolvedMethod();
            if (resolved.isEmpty()) {
                results.add(PesStepResult.failed(step.method(), "지원하지 않는 method"));
                aborted = true;
                continue;
            }

            LotMethodHandler handler = this.handlers.get(resolved.get());
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
