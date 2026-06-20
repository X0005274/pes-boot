package com.playtogether.pes.common.collaboration;

/**
 * 크로스 도메인 연계(파생 생성/전파) 중 발생한 실패를 나타낸다.
 * 호출 도메인의 workflow 처리에서 해당 step 을 FAILED 로 만든다.
 */
public class CollaborationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CollaborationException(String message) {
        super(message);
    }
}
