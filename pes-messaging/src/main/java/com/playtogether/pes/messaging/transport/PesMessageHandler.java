package com.playtogether.pes.messaging.transport;

/**
 * 인바운드 메시지를 처리하고 reply 페이로드(JSON, nullable)를 반환한다.
 */
@FunctionalInterface
public interface PesMessageHandler {

    String handle(PesInboundMessage inbound);
}
