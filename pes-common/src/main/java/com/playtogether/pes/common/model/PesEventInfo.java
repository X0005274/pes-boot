package com.playtogether.pes.common.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 공통 이벤트 정보. *_HIS 테이블의 EVENT 컬럼군에 매핑 가능한 구조.
 * eventTm 은 비어 있으면 서버에서 생성하는 것을 원칙으로 한다(여기선 값만 운반).
 */
@Schema(description = "이벤트 정보(*_HIS 기록)")
public record PesEventInfo(
        @Schema(description = "이벤트 코드", example = "CREATED", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String eventCd,
        @Schema(description = "이벤트 시각(없으면 서버 생성)")
        String eventTm,
        @Schema(description = "이벤트 설명")
        String eventDesc,
        @Schema(description = "상태 유형", example = "NEW")
        String statTyp
) {
}
