package com.playtogether.pes.hub;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * HubDB 적재 주기 실행. pes.hub.scheduler.enabled=true 일 때만 활성화된다.
 */
@Component
@ConditionalOnProperty(prefix = "pes.hub.scheduler", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class HubScheduler {

    private final HubIngestionService ingestionService;

    @Value("${pes.hub.scheduler.batch:200}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${pes.hub.scheduler.fixed-delay-ms:60000}")
    public void runIngestion() {
        this.ingestionService.ingest(this.batchSize);
    }

    /** 스케줄링 활성화(스케줄러 켜질 때만). */
    @Configuration
    @ConditionalOnProperty(prefix = "pes.hub.scheduler", name = "enabled", havingValue = "true")
    @EnableScheduling
    static class HubSchedulingConfig {
    }
}
