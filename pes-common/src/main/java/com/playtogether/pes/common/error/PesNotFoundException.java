package com.playtogether.pes.common.error;

/**
 * 조회 대상(도메인 마스터)이 없을 때. 진입점에서 404 로 환원한다.
 */
public class PesNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PesNotFoundException(String entityType, String entityId) {
        super(entityType + " not found: " + entityId);
    }
}
