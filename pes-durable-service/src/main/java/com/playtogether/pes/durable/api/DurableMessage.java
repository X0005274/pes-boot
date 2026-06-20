package com.playtogether.pes.durable.api;

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
 * DURABLE 도메인 최상위 메시지 DTO.
 * 계약: entityType 은 항상 "DURABLE". 필수: durableId, workflow, meta.srcSystem.
 * lotId 는 bindLot(makeInUse) 연계 시 사용.
 */
@Schema(description = "DURABLE 도메인 메시지(entityType 은 항상 DURABLE)")
public record DurableMessage(
        @Schema(description = "고정 값 DURABLE", example = "DURABLE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String entityType,
        @Schema(description = "DURABLE 식별자", example = "DUR001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String durableId,
        @Schema(description = "연계/바인딩 LOT 식별자", example = "LOT12345")
        String lotId,
        @Schema(description = "수행할 step 배열(1개 이상)")
        @NotEmpty @Valid List<DurableWorkflowStep> workflow,
        @NotNull @Valid PesMeta meta
) implements PesDomainMessage {

    public static final String ENTITY_TYPE_DURABLE = EntityType.DURABLE.name();

    public DurableMessage {
        workflow = (workflow == null) ? List.of() : List.copyOf(workflow);
    }

    @AssertTrue(message = "entityType must be 'DURABLE'")
    public boolean isDurableEntityType() {
        return ENTITY_TYPE_DURABLE.equals(this.entityType);
    }

    public void assertContract() {
        if (!ENTITY_TYPE_DURABLE.equals(this.entityType)) {
            throw new IllegalArgumentException(
                    "DURABLE 메시지의 entityType 은 'DURABLE' 이어야 합니다. 실제=" + this.entityType);
        }
    }

    @Override
    public EntityType resolveEntityType() {
        return EntityType.DURABLE;
    }

    @Override
    public String entityId() {
        return this.durableId;
    }

    @Override
    public DurableMessage withCorrelationId(String correlationId) {
        PesMeta newMeta = (this.meta != null)
                ? this.meta.withCorrelationId(correlationId)
                : new PesMeta(null, null, correlationId, null, null);
        return new DurableMessage(this.entityType, this.durableId, this.lotId, this.workflow, newMeta);
    }
}
