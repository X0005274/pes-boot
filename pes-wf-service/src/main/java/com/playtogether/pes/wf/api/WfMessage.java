package com.playtogether.pes.wf.api;

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
 * WF 도메인 최상위 메시지 DTO.
 * 계약: entityType 은 항상 "WF". 필수: wfId, workflow, meta.srcSystem.
 * lotId 는 LOT 파생(inheritLotSpec/applyToLot) 연계 시 사용.
 */
@Schema(description = "WF 도메인 메시지(entityType 은 항상 WF)")
public record WfMessage(
        @Schema(description = "고정 값 WF", example = "WF", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String entityType,
        @Schema(description = "WF 식별자", example = "WF56789", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String wfId,
        @Schema(description = "연계 LOT 식별자", example = "LOT12345")
        String lotId,
        @Schema(description = "수행할 step 배열(1개 이상)")
        @NotEmpty @Valid List<WfWorkflowStep> workflow,
        @NotNull @Valid PesMeta meta
) implements PesDomainMessage {

    public static final String ENTITY_TYPE_WF = EntityType.WF.name();

    public WfMessage {
        workflow = (workflow == null) ? List.of() : List.copyOf(workflow);
    }

    @AssertTrue(message = "entityType must be 'WF'")
    public boolean isWfEntityType() {
        return ENTITY_TYPE_WF.equals(this.entityType);
    }

    public void assertContract() {
        if (!ENTITY_TYPE_WF.equals(this.entityType)) {
            throw new IllegalArgumentException(
                    "WF 메시지의 entityType 은 'WF' 이어야 합니다. 실제=" + this.entityType);
        }
    }

    @Override
    public EntityType resolveEntityType() {
        return EntityType.WF;
    }

    @Override
    public String entityId() {
        return this.wfId;
    }

    @Override
    public WfMessage withCorrelationId(String correlationId) {
        PesMeta newMeta = (this.meta != null)
                ? this.meta.withCorrelationId(correlationId)
                : new PesMeta(null, null, correlationId, null, null);
        return new WfMessage(this.entityType, this.wfId, this.lotId, this.workflow, newMeta);
    }
}
