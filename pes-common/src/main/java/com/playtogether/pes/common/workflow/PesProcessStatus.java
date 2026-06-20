package com.playtogether.pes.common.workflow;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * workflow step / 전체 처리 결과 상태.
 */
@Schema(description = "처리 상태", enumAsRef = true)
public enum PesProcessStatus {

    @Schema(description = "성공")
    SUCCESS,

    @Schema(description = "실패(롤백 대상)")
    FAILED,

    @Schema(description = "이전 step 실패로 건너뜀")
    SKIPPED
}
