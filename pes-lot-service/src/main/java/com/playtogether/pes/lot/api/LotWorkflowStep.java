package com.playtogether.pes.lot.api;

import com.playtogether.pes.common.model.PesEventInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.Optional;

/**
 * workflow 배열의 단일 step. 항상 하나의 의미만 가진다.
 * method 는 wire 문자열로 운반하고, 검증은 {@link LotMethod#fromWire(String)} 로 한다.
 */
@Schema(description = "LOT workflow step(단일 작업)")
public record LotWorkflowStep(
        @Schema(description = "수행 작업", allowableValues = {"created", "released", "changeSpec"},
                example = "created", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String method,
        @Schema(description = "작업 옵션")
        LotStepOptions options,
        @Schema(description = "LOT_HIS 에 기록할 이벤트")
        @Valid PesEventInfo event
) {
    public LotWorkflowStep {
        if (options == null) {
            options = LotStepOptions.empty();
        }
    }

    /** 타입 안전 method 조회 (미지 값이면 empty) */
    public Optional<LotMethod> resolvedMethod() {
        return LotMethod.fromWire(this.method);
    }
}
