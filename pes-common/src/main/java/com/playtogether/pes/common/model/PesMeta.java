package com.playtogether.pes.common.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 공통 메타데이터. LOT/WF/DURABLE 메시지에서 재사용한다.
 * 메시지 계층 전용 불변 VO. (영속 계층은 별도 @Embeddable 사용 권장)
 */
@Schema(description = "공통 메타데이터")
public record PesMeta(
        @Schema(description = "출처 시스템", example = "UI", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String srcSystem,
        @Schema(description = "사용자 ID", example = "UIUSER01")
        String userId,
        @Schema(description = "상관관계 ID(없으면 서버가 PES-<uuid> 발급)", example = "TX-20260620-0001")
        String correlationId,
        @Schema(description = "요청 시각")
        String requestTm,
        @Schema(description = "로케일", example = "ko-KR")
        String locale
) {
    /** correlationId 만 교체한 사본 반환. */
    public PesMeta withCorrelationId(String newCorrelationId) {
        return new PesMeta(this.srcSystem, this.userId, newCorrelationId, this.requestTm, this.locale);
    }
}
