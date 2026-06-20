package com.playtogether.pes.common.workflow;

/**
 * 처리 실패로 트랜잭션을 롤백시키기 위한 예외(unchecked → @Transactional 기본 롤백 대상).
 * 크로스 도메인 원자성: 파생 도메인 처리가 같은 트랜잭션에 참여하므로,
 * 어느 step 이라도 실패하면 본/파생 변경이 모두 롤백된다.
 * 구조화된 결과를 운반해 진입점(디스패처/REST)에서 reply 로 환원할 수 있게 한다.
 */
public class PesProcessRollbackException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient PesProcessResult result;

    public PesProcessRollbackException(PesProcessResult result) {
        super("processing failed, rolling back: entityType=" + result.entityType()
                + ", entityId=" + result.entityId());
        this.result = result;
    }

    public PesProcessResult getResult() {
        return this.result;
    }
}
