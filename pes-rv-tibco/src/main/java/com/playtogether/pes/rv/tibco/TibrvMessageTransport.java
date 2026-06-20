package com.playtogether.pes.rv.tibco;

import com.playtogether.pes.messaging.transport.PesInboundMessage;
import com.playtogether.pes.messaging.transport.PesMessageHandler;
import com.playtogether.pes.messaging.transport.PesMessageTransport;
import com.tibco.tibrv.Tibrv;
import com.tibco.tibrv.TibrvDispatcher;
import com.tibco.tibrv.TibrvException;
import com.tibco.tibrv.TibrvListener;
import com.tibco.tibrv.TibrvMsg;
import com.tibco.tibrv.TibrvMsgCallback;
import com.tibco.tibrv.TibrvRvdTransport;
import com.tibco.tibrv.TibrvTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TIBCO Rendezvous 전송 구현. {@code rv} 프로파일에서만 활성화된다.
 * 인바운드 메시지의 JSON 은 RV 메시지의 "json" 필드(없으면 raw bytes)에서 읽고,
 * 처리 결과 JSON 은 RV Request/Reply(INBOX) 로 회신한다.
 *
 * <p>주의: 상용 tibrvj.jar 가 클래스패스에 있어야 컴파일/구동된다(profile: rv-tibco).
 */
@Component
@Profile("rv")
@EnableConfigurationProperties(TibrvProperties.class)
public class TibrvMessageTransport implements PesMessageTransport, TibrvMsgCallback {

    private static final Logger log = LoggerFactory.getLogger(TibrvMessageTransport.class);

    /** 페이로드 JSON 을 담는 RV 메시지 필드명. */
    public static final String FIELD_JSON = "json";

    private final TibrvProperties properties;
    private final Map<String, PesMessageHandler> handlers = new ConcurrentHashMap<>();

    private TibrvTransport transport;

    public TibrvMessageTransport(TibrvProperties properties) {
        this.properties = properties;
    }

    @Override
    public void subscribe(String subject, PesMessageHandler handler) {
        this.handlers.put(subject, handler);
    }

    @Override
    public void start() {
        try {
            Tibrv.open(Tibrv.IMPL_NATIVE);
            this.transport = new TibrvRvdTransport(
                    this.properties.getService(),
                    this.properties.getNetwork(),
                    this.properties.getDaemon());

            for (String subject : this.handlers.keySet()) {
                new TibrvListener(Tibrv.defaultQueue(), this, this.transport, subject, null);
                log.info("RV listener registered: {}", subject);
            }
            // 기본 큐를 처리하는 디스패처 스레드 기동
            new TibrvDispatcher(Tibrv.defaultQueue());
            log.info("TIBCO RV transport started. service={}, daemon={}",
                    this.properties.getService(), this.properties.getDaemon());
        } catch (TibrvException ex) {
            throw new IllegalStateException("TIBCO RV 전송 시작 실패", ex);
        }
    }

    @Override
    public void publish(String subject, String payload) {
        if (this.transport == null) {
            throw new IllegalStateException("RV transport 가 아직 시작되지 않았습니다");
        }
        try {
            TibrvMsg msg = new TibrvMsg();
            msg.setSendSubject(subject);
            msg.add(FIELD_JSON, payload);
            this.transport.send(msg);
        } catch (TibrvException ex) {
            throw new IllegalStateException("RV 이벤트 발행 실패: " + subject, ex);
        }
    }

    @Override
    public void onMsg(TibrvListener listener, TibrvMsg msg) {
        try {
            String subject = msg.getSendSubject();
            PesMessageHandler handler = this.handlers.get(subject);
            if (handler == null) {
                return;
            }

            String payload = extractPayload(msg);
            String replySubject = msg.getReplySubject();
            String reply = handler.handle(new PesInboundMessage(subject, replySubject, payload));

            if (replySubject != null && reply != null && this.transport != null) {
                TibrvMsg replyMsg = new TibrvMsg();
                replyMsg.add(FIELD_JSON, reply);
                this.transport.sendReply(replyMsg, msg);
            }
        } catch (TibrvException ex) {
            log.error("RV 메시지 처리 실패", ex);
        }
    }

    private String extractPayload(TibrvMsg msg) throws TibrvException {
        Object jsonField = msg.get(FIELD_JSON);
        if (jsonField != null) {
            return jsonField.toString();
        }
        return new String(msg.getAsBytes());
    }
}
