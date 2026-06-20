package com.playtogether.pes.hub.jdbc;

import com.playtogether.pes.hub.HubSource;
import com.playtogether.pes.hub.jdbc.cursor.PesHubCursorService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * HubDB JDBC 어댑터 설정. pes.hub.enabled=true 일 때만 활성화된다.
 * 자체 DataSource(DriverManagerDataSource)를 만들어 기본 PES DataSource 자동설정에 영향을 주지 않는다.
 * 적재 방식은 pes.hub.mode 로 선택: "mas"(초기적재, 기본) | "his"(HIS 기반 증분 replay).
 */
@Configuration
@ConditionalOnProperty(prefix = "pes.hub", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(HubDataSourceProperties.class)
public class HubJdbcConfig {

    @Bean
    public JdbcTemplate hubJdbcTemplate(HubDataSourceProperties properties) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        if (properties.getDriverClassName() != null && !properties.getDriverClassName().isBlank()) {
            dataSource.setDriverClassName(properties.getDriverClassName());
        }
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @ConditionalOnProperty(prefix = "pes.hub", name = "mode", havingValue = "mas", matchIfMissing = true)
    public HubSource masHubSource(JdbcTemplate hubJdbcTemplate) {
        return new MasHubSource(hubJdbcTemplate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "pes.hub", name = "mode", havingValue = "his")
    public HubSource historyHubSource(JdbcTemplate hubJdbcTemplate, PesHubCursorService cursors) {
        return new HistoryHubSource(hubJdbcTemplate, cursors);
    }
}
