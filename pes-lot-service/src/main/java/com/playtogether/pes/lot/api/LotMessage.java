package com.playtogether.pes.lot.api;

import com.playtogether.pes.common.model.EntityType;
import com.playtogether.pes.common.model.PesDomainMessage;
import com.playtogether.pes.common.model.PesMeta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * LOT 도메인 최상위 메시지 DTO.
 * 계약: entityType 은 항상 "LOT".
 */
@Schema(description = "LOT 도메인 메시지(entityType 은 항상 LOT)")
public record LotMessage(
        @Schema(description = "고정 값 LOT", example = "LOT", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String entityType,
        @Schema(description = "LOT 식별자", example = "LOT12345", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String lotId,
        @Schema(description = "연계 WF 식별자", example = "WF56789")
        String wfId,
        @Schema(description = "연계 DURABLE 식별자", example = "DUR001")
        String durableId,
        @Schema(description = "수행할 step 배열(1개 이상)")
        @NotEmpty @Valid List<LotWorkflowStep> workflow,
        @NotNull @Valid PesMeta meta
) implements PesDomainMessage {
    /** LOT 계약 고정 값 */
    public static final String ENTITY_TYPE_LOT = EntityType.LOT.name();

    public LotMessage {
        // workflow null 방지 + 불변화 (빈 여부는 @NotEmpty 로 검증)
        workflow = (workflow == null) ? List.of() : List.copyOf(workflow);
    }

    /** Bean Validation 으로 entityType 계약을 검증한다. */
    @AssertTrue(message = "entityType must be 'LOT'")
    public boolean isLotEntityType() {
        return ENTITY_TYPE_LOT.equals(this.entityType);
    }

    @Override
    public EntityType resolveEntityType() {
        return EntityType.LOT;
    }

    @Override
    public String entityId() {
        return this.lotId;
    }

    @Override
    public LotMessage withCorrelationId(String correlationId) {
        PesMeta newMeta = (this.meta != null)
                ? this.meta.withCorrelationId(correlationId)
                : new PesMeta(null, null, correlationId, null, null);
        return new LotMessage(this.entityType, this.lotId, this.wfId, this.durableId,
                this.workflow, newMeta);
    }

    /**
     * 코드 수준에서 LOT 계약을 강제. 신뢰 가능한 경로(팩토리/검증 통과 후)에서 호출.
     */
    public void assertContract() {
        if (!ENTITY_TYPE_LOT.equals(this.entityType)) {
            throw new IllegalArgumentException(
                    "LOT 메시지의 entityType 은 'LOT' 이어야 합니다. 실제=" + this.entityType);
        }
    }
}
