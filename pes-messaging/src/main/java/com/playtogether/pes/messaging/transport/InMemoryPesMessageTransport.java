package com.playtogether.pes.messaging.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 기본/테스트용 in-memory 전송. 실제 RV 가 없는 환경에서 인바운드/아웃바운드 파이프라인을 구동/검증한다.
 * {@link #simulateInbound(String, String)} 로 수신을 흉내내고, 발행 이벤트는 {@link #publishedEvents()} 로 확인한다.
 */
public class InMemoryPesMessageTransport implements PesMessageTransport {

    private static final Logger log = LoggerFactory.getLogger(InMemoryPesMessageTransport.class);

    /** 발행 이벤트 기록(테스트 검증용). */
    public record PublishedEvent(String subject, String payload) {
    }

    private final Map<String, PesMessageHandler> handlers = new ConcurrentHashMap<>();
    private final List<PublishedEvent> published = new CopyOnWriteArrayList<>();
    private volatile boolean started;

    @Override
    public void subscribe(String subject, PesMessageHandler handler) {
        this.handlers.put(subject, handler);
    }

    @Override
    public void start() {
        this.started = true;
        log.info("In-memory transport started. subjects={}", this.handlers.keySet());
    }

    @Override
    public void publish(String subject, String payload) {
        this.published.add(new PublishedEvent(subject, payload));
        log.info("Event published -> {}", subject);
    }

    /** 발행된 이벤트 목록(테스트 검증용). */
    public List<PublishedEvent> publishedEvents() {
        return List.copyOf(this.published);
    }

    @Override
    public String request(String subject, String payload, long timeoutMillis) {
        // in-memory: 동일 JVM 의 구독 핸들러를 동기 호출해 reply 를 반환(2-hop 시뮬레이션)
        PesMessageHandler handler = this.handlers.get(subject);
        if (handler == null) {
            throw new IllegalStateException("구독자가 없는 subject: " + subject);
        }
        return handler.handle(new PesInboundMessage(subject, "_INBOX.REQ", payload));
    }

    /** 인바운드 수신 시뮬레이션. 등록된 핸들러를 호출하고 reply 페이로드를 반환한다. */
    public String simulateInbound(String subject, String payload) {
        PesMessageHandler handler = this.handlers.get(subject);
        if (handler == null) {
            throw new IllegalStateException("구독자가 없는 subject: " + subject);
        }
        return handler.handle(new PesInboundMessage(subject, "_INBOX.TEST", payload));
    }

    public boolean isStarted() {
        return this.started;
    }
}
