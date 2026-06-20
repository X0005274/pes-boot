package com.playtogether.pes.common.workflow;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 메시지 1건(여러 step 포함)에 대한 전체 처리 결과. RV Reply 본문으로 사용 가능.
 */
@Schema(description = "메시지 처리 결과(전체 + step 별)")
public record PesProcessResult(
        @Schema(example = "LOT") String entityType,
        @Schema(description = "도메인 식별자", example = "LOT12345") String entityId,
        @Schema(description = "상관관계 ID") String correlationId,
        @Schema(description = "전체 상태") PesProcessStatus status,
        @Schema(description = "step 별 결과") List<PesStepResult> steps
) {
    public PesProcessResult {
        steps = (steps == null) ? List.of() : List.copyOf(steps);
    }
}
