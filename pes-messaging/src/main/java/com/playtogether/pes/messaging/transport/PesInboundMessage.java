package com.playtogether.pes.messaging.transport;

/**
 * 전송 계층에서 수신한 원시 인바운드 메시지. (RV/in-memory 공통)
 *
 * @param subject      수신 subject (예: PES.BIZ.LOT.EVENT)
 * @param replySubject Reply 대상 (RV _INBOX). 없으면 null
 * @param payload      메시지 본문(JSON)
 */
public record PesInboundMessage(String subject, String replySubject, String payload) {
}
