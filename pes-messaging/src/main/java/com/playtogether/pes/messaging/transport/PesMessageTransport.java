package com.playtogether.pes.messaging.transport;

/**
 * 메시지 전송 포트. 구현체로 TIBCO RV(TibrvMessageTransport) 또는 in-memory(테스트/기본) 가 있다.
 */
public interface PesMessageTransport {

    /** subject 에 대한 핸들러를 등록한다. start() 이전에 호출한다. */
    void subscribe(String subject, PesMessageHandler handler);

    /** 전송 계층을 기동하고 수신을 시작한다. */
    void start();

    /** subject 로 이벤트(payload JSON)를 단방향 발행한다 (Biz → UI push). */
    void publish(String subject, String payload);

    /**
     * subject 로 Request 를 보내고 Reply 페이로드를 받는다(Request/Reply, INBOX).
     * 포워더(UI.REQUEST → BIZ.EVENT)에서 사용. 타임아웃 시 null.
     */
    String request(String subject, String payload, long timeoutMillis);
}
