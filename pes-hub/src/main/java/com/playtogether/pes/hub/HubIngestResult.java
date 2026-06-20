package com.playtogether.pes.hub;

/**
 * HubDB 적재 배치 결과 요약.
 *
 * @param total   처리 시도 건수
 * @param success 성공(중복 무시 포함, 결과 status=SUCCESS)
 * @param failed  실패(롤백된 건)
 */
public record HubIngestResult(int total, int success, int failed) {
}
