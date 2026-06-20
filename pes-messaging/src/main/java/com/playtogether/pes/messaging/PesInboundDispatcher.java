package com.playtogether.pes.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playtogether.pes.common.messaging.PesMessageType;
import com.playtogether.pes.common.model.PesDomainMessage;
import com.playtogether.pes.common.workflow.PesMessageProcessor;
import com.playtogether.pes.common.workflow.PesProcessResult;
import com.playtogether.pes.common.workflow.PesProcessRollbackException;
import com.playtogether.pes.common.workflow.PesProcessStatus;
import com.playtogether.pes.common.workflow.PesStepResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 전송 무관 인바운드 디스패처. subject 로 도메인 메시지 클래스를 찾아 역직렬화·검증 후
 * PesDomainRouter 로 라우팅하고, 처리 결과(PesProcessResult)를 JSON 으로 반환한다.
 * 실패(미구독 subject / 역직렬화 / 검증 / 처리)는 FAILED 결과 JSON 으로 환원한다.
 */
@Component
public class PesInboundDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PesInboundDispatcher.class);

    private final PesMessageProcessor processor;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final Map<String, PesMessageType> typesBySubject;

    public PesInboundDispatcher(PesMessageProcessor processor,
                                ObjectMapper objectMapper,
                                Validator validator,
                                List<PesMessageType> messageTypes) {
        this.processor = processor;
        this.objectMapper = objectMapper;
        this.validator = validator;
        Map<String, PesMessageType> map = new HashMap<>();
        for (PesMessageType type : messageTypes) {
            map.put(type.inboundSubject(), type);
        }
        this.typesBySubject = Map.copyOf(map);
        log.info("Inbound subjects registered: {}", this.typesBySubject.keySet());
    }

    public String onMessage(String subject, String payload) {
        PesMessageType type = this.typesBySubject.get(subject);
        if (type == null) {
            return errorJson("UNKNOWN", "구독되지 않은 subject: " + subject);
        }

        PesDomainMessage message;
        try {
            message = this.objectMapper.readValue(payload, type.messageClass());
        } catch (Exception ex) {
            return errorJson(type.entityType().name(), "역직렬화 실패: " + ex.getMessage());
        }

        Set<ConstraintViolation<PesDomainMessage>> violations = this.validator.validate(message);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            return errorJson(type.entityType().name(), "검증 실패: " + detail);
        }

        try {
            // process → (멱등 검사) → route → 도메인 서비스가 성공 시 이벤트를 발행한다.
            PesProcessResult result = this.processor.process(message);
            return toJson(result);
        } catch (PesProcessRollbackException ex) {
            // 처리 실패로 트랜잭션이 롤백됨. 구조화된 결과를 reply 로 환원(이벤트는 미발행).
            return toJson(ex.getResult());
        } catch (RuntimeException ex) {
            log.warn("Inbound 처리 실패 subject={}", subject, ex);
            return errorJson(type.entityType().name(), "처리 실패: " + ex.getMessage());
        }
    }

    private String errorJson(String entityType, String reason) {
        PesProcessResult result = new PesProcessResult(
                entityType, null, null, PesProcessStatus.FAILED,
                List.of(PesStepResult.failed("inbound", reason)));
        return toJson(result);
    }

    private String toJson(PesProcessResult result) {
        try {
            return this.objectMapper.writeValueAsString(result);
        } catch (Exception ex) {
            log.error("Reply 직렬화 실패", ex);
            return "{\"status\":\"FAILED\",\"steps\":[{\"method\":\"inbound\","
                    + "\"status\":\"FAILED\",\"message\":\"reply 직렬화 실패\"}]}";
        }
    }
}
