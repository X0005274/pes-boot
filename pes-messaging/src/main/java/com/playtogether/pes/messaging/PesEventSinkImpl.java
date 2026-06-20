package com.playtogether.pes.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playtogether.pes.common.messaging.PesEventSink;
import com.playtogether.pes.common.messaging.PesMessageType;
import com.playtogether.pes.common.model.EntityType;
import com.playtogether.pes.common.workflow.PesProcessResult;
import com.playtogether.pes.messaging.transport.PesMessageTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 아웃바운드 이벤트 발행 구현. EntityType → 이벤트 subject(PES.UI.&lt;domain&gt;.EVENT)를
 * 도메인 SPI(PesMessageType)에서 구성하고, 전송 포트로 push 한다.
 *
 * <p>트랜잭션 정합: 활성 트랜잭션이 있으면 <b>커밋 성공 후(afterCommit)</b>에 발행한다.
 * 롤백 시에는 발행하지 않으므로 "DB 미반영인데 이벤트만 나가는" 불일치를 방지한다.
 * 트랜잭션이 없으면 즉시 발행한다. 발행 실패는 본 처리에 영향을 주지 않도록 삼키고 로깅만 한다.
 */
@Component
public class PesEventSinkImpl implements PesEventSink {

    private static final Logger log = LoggerFactory.getLogger(PesEventSinkImpl.class);

    private final PesMessageTransport transport;
    private final ObjectMapper objectMapper;
    private final Map<EntityType, String> eventSubjectByEntity;

    public PesEventSinkImpl(PesMessageTransport transport,
                            ObjectMapper objectMapper,
                            List<PesMessageType> messageTypes) {
        this.transport = transport;
        this.objectMapper = objectMapper;
        Map<EntityType, String> map = new EnumMap<>(EntityType.class);
        for (PesMessageType type : messageTypes) {
            map.put(type.entityType(), type.eventSubject());
        }
        this.eventSubjectByEntity = map;
    }

    @Override
    public void publishResult(EntityType domain, PesProcessResult result) {
        String subject = this.eventSubjectByEntity.get(domain);
        if (subject == null) {
            log.warn("이벤트 subject 미등록 도메인: {}", domain);
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 커밋 성공 후에만 발행 (파생 도메인 이벤트도 동일 트랜잭션 커밋 시 함께 발행됨)
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublish(subject, result);
                }
            });
        } else {
            doPublish(subject, result);
        }
    }

    private void doPublish(String subject, PesProcessResult result) {
        try {
            this.transport.publish(subject, this.objectMapper.writeValueAsString(result));
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            log.warn("이벤트 발행 실패 subject={}", subject, ex);
        }
    }
}
