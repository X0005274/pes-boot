package com.playtogether.pes.lot.service;

import com.playtogether.pes.common.messaging.PesEventSink;
import com.playtogether.pes.common.model.EntityType;
import com.playtogether.pes.common.workflow.PesDomainHandler;
import com.playtogether.pes.common.workflow.PesProcessResult;
import com.playtogether.pes.common.workflow.PesProcessRollbackException;
import com.playtogether.pes.common.workflow.PesProcessStatus;
import com.playtogether.pes.common.workflow.PesStepResult;
import com.playtogether.pes.lot.api.LotMessage;
import com.playtogether.pes.lot.workflow.LotWorkflowHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * LOT 도메인 진입점. {@code PesDomainRouter} 가 EntityType.LOT 메시지를 이 빈으로 라우팅한다.
 * entityType 계약 검증 후 workflow 를 순차 처리하고 전체 결과를 집계한다.
 */
@Service
@RequiredArgsConstructor
public class LotMessageService implements PesDomainHandler<LotMessage> {

    private final LotWorkflowHandler workflowHandler;
    private final ObjectProvider<PesEventSink> eventSink;

    @Override
    public EntityType entityType() {
        return EntityType.LOT;
    }

    // workflow 전체를 단일 트랜잭션으로 처리. step 이 FAILED 를 "반환"하면 abort 후
    // 그때까지의 변경은 커밋된다(예외를 던지지 않으므로 롤백되지 않음).
    // 전부-또는-전무 롤백이 필요하면 추후 정책 확정 후 예외 기반으로 전환한다.
    @Override
    @Transactional
    public PesProcessResult handle(LotMessage message) {
        message.assertContract();

        List<PesStepResult> steps = this.workflowHandler.process(message);
        PesProcessStatus overall = resolveOverall(steps);

        PesProcessResult result = new PesProcessResult(
                message.entityType(),
                message.lotId(),
                message.meta() != null ? message.meta().correlationId() : null,
                overall,
                steps);

        // 크로스 도메인 원자성: 실패 시 예외로 전체 롤백(파생 도메인 변경 포함). 이벤트도 미발행.
        if (overall != PesProcessStatus.SUCCESS) {
            throw new PesProcessRollbackException(result);
        }

        // 성공 시 상태 변경 push (PES.UI.LOT.EVENT, afterCommit). 메시징 계층이 없으면 무시.
        PesEventSink sink = this.eventSink.getIfAvailable();
        if (sink != null) {
            sink.publishResult(EntityType.LOT, result);
        }
        return result;
    }

    private PesProcessStatus resolveOverall(List<PesStepResult> steps) {
        for (PesStepResult step : steps) {
            if (step.status() == PesProcessStatus.FAILED) {
                return PesProcessStatus.FAILED;
            }
        }
        return PesProcessStatus.SUCCESS;
    }
}
