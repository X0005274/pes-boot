package com.playtogether.pes.hub;

import com.playtogether.pes.common.model.PesDomainMessage;

import java.util.List;

/**
 * 파티션(도메인) 단위로 폴링 가능한 소스. HubIngestionService 가 파티션별로 병렬 적재하며
 * 파티션별 잠금 키로 직렬화한다. (예: LOT/WF/DURABLE 병렬, 동일 도메인은 직렬)
 */
public interface PartitionedHubSource extends HubSource {

    /** 적재 파티션 목록(예: ["LOT","WF","DURABLE"]). */
    List<String> partitions();

    /** 특정 파티션의 다음 배치. */
    List<PesDomainMessage> poll(String partition, int maxBatch);
}
