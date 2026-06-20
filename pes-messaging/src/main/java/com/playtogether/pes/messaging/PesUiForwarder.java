package com.playtogether.pes.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playtogether.pes.common.messaging.PesMessageType;
import com.playtogether.pes.common.model.PesDomainMessage;
import com.playtogether.pes.common.workflow.PesProcessResult;
import com.playtogether.pes.common.workflow.PesProcessStatus;
import com.playtogether.pes.common.workflow.PesStepResult;
import com.playtogether.pes.messaging.transport.PesMessageTransport;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PES.UI 포워더. UI 요청(PES.UI.&lt;domain&gt;.REQUEST)을 수신해 <b>검증</b> 후
 * 동일 본문을 Biz(PES.BIZ.&lt;domain&gt;.EVENT)로 Request/Reply 전달하고, Reply 를 UI 로 릴레이한다.
 * 비즈니스 처리는 하지 않으며 라우팅/검증/전달만 담당한다.
 *
 * <p>별도 배포 레이어이므로 기본 비활성(pes.forwarder.enabled=true 일 때만 동작).
 * Biz 디스패처와 한 프로세스에 함께 두지 않는 것을 권장(역할 분리).
 */
@Component
@ConditionalOnProperty(prefix = "pes.forwarder", name = "enabled", havingValue = "true")
public class PesUiForwarder {

    private static final Logger log = LoggerFactory.getLogger(PesUiForwarder.class);

    private final PesMessageTransport transport;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final List<PesMessageType> messageTypes;

    @Value("${pes.forwarder.timeout-ms:10000}")
    private long timeoutMillis;

    public PesUiForwarder(PesMessageTransport transport, ObjectMapper objectMapper,
                          Validator validator, List<PesMessageType> messageTypes) {
        this.transport = transport;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.messageTypes = messageTypes;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void subscribeAll() {
        for (PesMessageType type : this.messageTypes) {
            this.transport.subscribe(type.requestSubject(), inbound -> forward(type, inbound.payload()));
            log.info("Forwarder subscribed {} -> {}", type.requestSubject(), type.inboundSubject());
        }
        this.transport.start();
    }

    private String forward(PesMessageType type, String payload) {
        // 1) 검증(동일 스키마). 실패 시 Biz 로 전달하지 않고 즉시 실패 응답.
        String validationError = validate(type, payload);
        if (validationError != null) {
            return errorJson(type, validationError);
        }
        // 2) 동일 본문을 Biz 로 전달하고 Reply 릴레이
        try {
            String reply = this.transport.request(type.inboundSubject(), payload, this.timeoutMillis);
            return (reply != null) ? reply : errorJson(type, "Biz 응답 타임아웃");
        } catch (RuntimeException ex) {
            log.warn("Forward 실패 {} → {}", type.requestSubject(), type.inboundSubject(), ex);
            return errorJson(type, "전달 실패: " + ex.getMessage());
        }
    }

    private String validate(PesMessageType type, String payload) {
        PesDomainMessage message;
        try {
            message = this.objectMapper.readValue(payload, type.messageClass());
        } catch (Exception ex) {
            return "역직렬화 실패: " + ex.getMessage();
        }
        Set<ConstraintViolation<PesDomainMessage>> violations = this.validator.validate(message);
        if (violations.isEmpty()) {
            return null;
        }
        return "검증 실패: " + violations.stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("; "));
    }

    private String errorJson(PesMessageType type, String reason) {
        PesProcessResult result = new PesProcessResult(
                type.entityType().name(), null, null, PesProcessStatus.FAILED,
                List.of(PesStepResult.failed("forward", reason)));
        try {
            return this.objectMapper.writeValueAsString(result);
        } catch (Exception ex) {
            return "{\"status\":\"FAILED\",\"steps\":[{\"method\":\"forward\",\"status\":\"FAILED\"}]}";
        }
    }
}
