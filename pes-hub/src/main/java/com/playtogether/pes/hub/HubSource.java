package com.playtogether.pes.hub;

import com.playtogether.pes.common.model.PesDomainMessage;

import java.util.List;

/**
 * HubDB 입력 소스 포트. HubDB(PES 와 동일 스키마, 읽기 전용)에서 읽은 레코드를
 * PES 도메인 메시지(srcSystem="HUB")로 매핑해 배치로 제공한다.
 * 실제 구현은 2nd DataSource/JPA 로 HubDB 를 읽어 매핑한다(별도 제공).
 */
@FunctionalInterface
public interface HubSource {

    /** 다음 적재 배치를 최대 maxBatch 건까지 가져온다. 더 없으면 빈 리스트. */
    List<PesDomainMessage> poll(int maxBatch);
}
