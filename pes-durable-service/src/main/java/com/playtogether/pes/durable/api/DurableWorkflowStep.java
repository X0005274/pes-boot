package com.playtogether.pes.durable.api;

import com.playtogether.pes.common.model.PesEventInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.Optional;

/**
 * DURABLE workflow 배열의 단일 step. (LOT/WF 와 동일 패턴)
 */
@Schema(description = "DURABLE workflow step(단일 작업)")
public record DurableWorkflowStep(
        @Schema(description = "수행 작업", allowableValues = {"created", "makeInUse", "changeSpec"},
                example = "created", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String method,
        @Schema(description = "작업 옵션")
        DurableStepOptions options,
        @Schema(description = "DURABLE_HIS 에 기록할 이벤트")
        @Valid PesEventInfo event
) {
    public DurableWorkflowStep {
        if (options == null) {
            options = DurableStepOptions.empty();
        }
    }

    public Optional<DurableMethod> resolvedMethod() {
        return DurableMethod.fromWire(this.method);
    }
}
