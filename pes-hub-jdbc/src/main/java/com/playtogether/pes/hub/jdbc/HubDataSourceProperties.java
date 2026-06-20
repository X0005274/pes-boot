package com.playtogether.pes.hub.jdbc;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HubDB 접속 정보(pes.hub.datasource.*). 모두 OS 환경변수 주입 권장.
 * 기본 PES DataSource 자동설정을 깨지 않도록, 이 정보로 어댑터가 자체 DataSource 를 만든다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "pes.hub.datasource")
public class HubDataSourceProperties {

    private String url;
    private String username;
    private String password;
    private String driverClassName;
}
