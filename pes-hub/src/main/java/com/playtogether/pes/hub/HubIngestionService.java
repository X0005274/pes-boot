package com.playtogether.pes.hub;

import com.playtogether.pes.common.model.PesDomainMessage;
import com.playtogether.pes.common.model.PesMeta;
import com.playtogether.pes.common.workflow.PesMessageProcessor;
import com.playtogether.pes.common.workflow.PesProcessResult;
import com.playtogether.pes.common.workflow.PesProcessRollbackException;
import com.playtogether.pes.common.workflow.PesProcessStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * HubDB → PES 적재 서비스. HubSource 에서 배치를 가져와 <b>동일 Biz 파이프라인</b>으로 처리한다.
 * 레코드별 독립 처리/트랜잭션이라 한 건 실패가 배치 전체를 중단시키지 않으며,
 * correlationId 안정성으로 재적재가 멱등이다.
 *
 * <p>소스가 {@link PartitionedHubSource} 이면 파티션(도메인)별로 <b>병렬 적재</b>하고,
 * 파티션별 잠금 키("his:ingest-&lt;partition&gt;")로 직렬화한다 → 도메인 간 병렬, 동일 도메인 직렬.
 */
@Service
@RequiredArgsConstructor
public class HubIngestionService {

    private static final Logger log = LoggerFactory.getLogger(HubIngestionService.class);
    private static final String GLOBAL_LOCK_KEY = "his:ingest-lock";
    private static final String PARTITION_LOCK_PREFIX = "his:ingest-";

    private final HubSource source;
    private final PesMessageProcessor processor;
    private final ObjectProvider<HubIngestLock> ingestLock;

    public HubIngestResult ingest(int maxBatch) {
        HubIngestLock lock = this.ingestLock.getIfAvailable();
        if (this.source instanceof PartitionedHubSource partitioned) {
            return ingestPartitioned(partitioned, lock, maxBatch);
        }
        // 단일(비파티션) 소스: 전역 잠금(있으면) 하에 전체 적재
        if (lock != null) {
            return lock.runExclusively(GLOBAL_LOCK_KEY, () -> processBatch(this.source.poll(maxBatch)));
        }
        return processBatch(this.source.poll(maxBatch));
    }

    private HubIngestResult ingestPartitioned(PartitionedHubSource partitioned,
                                              HubIngestLock lock, int maxBatch) {
        List<String> partitions = partitioned.partitions();
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, partitions.size()));
        try {
            List<Future<HubIngestResult>> futures = new ArrayList<>(partitions.size());
            for (String partition : partitions) {
                Supplier<HubIngestResult> work =
                        () -> processBatch(partitioned.poll(partition, maxBatch));
                futures.add(pool.submit(() ->
                        (lock != null)
                                ? lock.runExclusively(PARTITION_LOCK_PREFIX + partition, work)
                                : work.get()));
            }
            return aggregate(futures);
        } finally {
            pool.shutdownNow();
        }
    }

    private HubIngestResult aggregate(List<Future<HubIngestResult>> futures) {
        int total = 0;
        int success = 0;
        int failed = 0;
        for (Future<HubIngestResult> future : futures) {
            try {
                HubIngestResult r = future.get();
                total += r.total();
                success += r.success();
                failed += r.failed();
            } catch (ExecutionException ex) {
                throw new IllegalStateException("Hub 파티션 적재 실패", ex.getCause());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Hub 적재 중단됨", ex);
            }
        }
        return new HubIngestResult(total, success, failed);
    }

    private HubIngestResult processBatch(List<PesDomainMessage> batch) {
        int success = 0;
        int failed = 0;
        List<HubAck> acks = new ArrayList<>(batch.size());

        for (PesDomainMessage message : batch) {
            boolean ok;
            try {
                PesProcessResult result = this.processor.process(message);
                ok = result.status() == PesProcessStatus.SUCCESS;
            } catch (PesProcessRollbackException ex) {
                ok = false;
                log.warn("Hub 적재 실패(롤백): {}", ex.getMessage());
            } catch (RuntimeException ex) {
                ok = false;
                log.warn("Hub 적재 중 예외", ex);
            }

            if (ok) {
                success++;
            } else {
                failed++;
            }
            acks.add(new HubAck(correlationIdOf(message), ok));
        }

        // at-least-once: 처리 결과 기반으로 워터마크 전진(성공 연속 구간까지만)
        if (this.source instanceof HubAckSource ackSource) {
            ackSource.acknowledge(acks);
        }

        log.info("Hub 적재 배치 완료: total={}, success={}, failed={}", batch.size(), success, failed);
        return new HubIngestResult(batch.size(), success, failed);
    }

    private static String correlationIdOf(PesDomainMessage message) {
        PesMeta meta = message.meta();
        return (meta != null) ? meta.correlationId() : null;
    }
}
