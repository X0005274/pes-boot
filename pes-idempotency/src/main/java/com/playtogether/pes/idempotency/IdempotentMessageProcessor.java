package com.playtogether.pes.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playtogether.pes.common.model.PesDomainMessage;
import com.playtogether.pes.common.observability.PesCorrelationIds;
import com.playtogether.pes.common.support.PesTimekeyGenerator;
import com.playtogether.pes.common.workflow.PesDomainRouter;
import com.playtogether.pes.common.workflow.PesMessageProcessor;
import com.playtogether.pes.common.workflow.PesProcessResult;
import com.playtogether.pes.idempotency.entity.PesProcessedMessage;
import com.playtogether.pes.idempotency.repository.PesProcessedMessageRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * correlationId 기반 멱등 처리 데코레이터(@Primary).
 * 처리와 멱등 마커 저장을 한 트랜잭션으로 묶는다.
 * <ul>
 *   <li>최초 수신: 라우팅 처리 → 성공 시 마커 저장(같은 트랜잭션 커밋)</li>
 *   <li>중복 수신: 저장된 결과를 재처리 없이 반환(부수효과 없음)</li>
 *   <li>처리 실패: 예외 전파로 전체 롤백(마커 미저장) → 재시도 가능</li>
 *   <li>correlationId 없음: 멱등 불가 → 일반 처리</li>
 * </ul>
 */
@Service
@Primary
@RequiredArgsConstructor
public class IdempotentMessageProcessor implements PesMessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(IdempotentMessageProcessor.class);

    private final PesDomainRouter router;
    private final PesProcessedMessageRepository repository;
    private final ObjectMapper objectMapper;
    private final PesTimekeyGenerator timekeyGenerator;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PesProcessResult process(PesDomainMessage rawMessage) {
        // correlationId 가 없으면 서버 발급 → 멱등 키/추적/응답에 일관 적용
        PesDomainMessage message = PesCorrelationIds.ensure(rawMessage);
        String correlationId = message.meta().correlationId();

        Optional<PesProcessedMessage> existing = this.repository.findById(correlationId);
        if (existing.isPresent()) {
            log.info("중복 메시지 무시(idempotent): correlationId={}", correlationId);
            return deserialize(existing.get());
        }

        // 최초 처리 (실패 시 PesProcessRollbackException 전파 → 마커 포함 전체 롤백)
        PesProcessResult result = this.router.route(message);

        this.repository.save(new PesProcessedMessage(
                correlationId,
                message.resolveEntityType() != null ? message.resolveEntityType().name() : null,
                message.entityId(),
                result.status().name(),
                serialize(result),
                this.timekeyGenerator.currentTimestamp()));
        return result;
    }

    private String serialize(PesProcessResult result) {
        try {
            return this.objectMapper.writeValueAsString(result);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("멱등 결과 직렬화 실패", ex);
        }
    }

    private PesProcessResult deserialize(PesProcessedMessage marker) {
        try {
            return this.objectMapper.readValue(marker.getResultJson(), PesProcessResult.class);
        } catch (Exception ex) {
            throw new IllegalStateException("멱등 결과 역직렬화 실패: correlationId="
                    + marker.getCorrelationId(), ex);
        }
    }
}
