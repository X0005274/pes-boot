package com.playtogether.pes.messaging;

import com.playtogether.pes.common.messaging.PesMessageType;
import com.playtogether.pes.messaging.transport.PesMessageTransport;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 애플리케이션 기동 완료 시, 등록된 모든 도메인 메시지 타입의 inbound subject 를
 * 전송 계층에 구독시키고 수신을 시작한다.
 */
@Component
@RequiredArgsConstructor
public class PesInboundSubscriber {

    private static final Logger log = LoggerFactory.getLogger(PesInboundSubscriber.class);

    private final PesMessageTransport transport;
    private final PesInboundDispatcher dispatcher;
    private final List<PesMessageType> messageTypes;

    @EventListener(ApplicationReadyEvent.class)
    public void subscribeAll() {
        for (PesMessageType type : this.messageTypes) {
            String subject = type.inboundSubject();
            this.transport.subscribe(subject,
                    inbound -> this.dispatcher.onMessage(inbound.subject(), inbound.payload()));
            log.info("Subscribed {} -> {}", subject, type.entityType());
        }
        this.transport.start();
    }
}
