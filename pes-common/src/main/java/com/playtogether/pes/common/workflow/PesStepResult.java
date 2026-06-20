package com.playtogether.pes.common.workflow;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 단일 workflow step 처리 결과. LOT/WF/DURABLE 공통.
 */
@Schema(description = "step 처리 결과")
public record PesStepResult(
        @Schema(description = "작업", example = "created") String method,
        @Schema(description = "상태") PesProcessStatus status,
        @Schema(description = "결과 메시지") String message
) {
    public static PesStepResult success(String method, String message) {
        return new PesStepResult(method, PesProcessStatus.SUCCESS, message);
    }

    public static PesStepResult failed(String method, String message) {
        return new PesStepResult(method, PesProcessStatus.FAILED, message);
    }

    public static PesStepResult skipped(String method) {
        return new PesStepResult(method, PesProcessStatus.SKIPPED, "이전 step 실패로 건너뜀");
    }
}
