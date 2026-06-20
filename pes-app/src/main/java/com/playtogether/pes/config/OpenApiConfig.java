package com.playtogether.pes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc 자동생성 문서 메타데이터. /v3/api-docs (스펙) 및 /swagger-ui.html (UI) 로 제공된다.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pesOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PES (Project Execution System) API")
                .description("LOT/WF/DURABLE 도메인의 명령(workflow)·조회(상태/이력)·HubDB 적재 API")
                .version("0.0.1"));
    }
}
