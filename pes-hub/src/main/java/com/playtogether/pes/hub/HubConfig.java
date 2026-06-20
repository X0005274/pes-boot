package com.playtogether.pes.hub;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Hub 기본 설정. Hub 적재가 비활성(pes.hub.enabled 미설정/false)일 때만 빈 소스를 제공한다.
 * 활성화 시에는 어댑터 모듈(pes-hub-jdbc 등)이 실제 HubSource 를 등록한다.
 */
@Configuration
public class HubConfig {

    @Bean
    @ConditionalOnProperty(prefix = "pes.hub", name = "enabled", havingValue = "false", matchIfMissing = true)
    public HubSource emptyHubSource() {
        return maxBatch -> List.of();
    }
}
