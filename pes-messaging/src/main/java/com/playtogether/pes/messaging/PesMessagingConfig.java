package com.playtogether.pes.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.playtogether.pes.messaging.transport.InMemoryPesMessageTransport;
import com.playtogether.pes.messaging.transport.PesMessageTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 메시징 기본 설정. 실제 전송 구현(TIBCO 등)이 빈으로 없으면 in-memory 전송으로 대체하고,
 * 컨텍스트에 ObjectMapper 빈이 없으면(모듈 단독 구동 등) 기본 매퍼를 제공한다.
 */
@Configuration
public class PesMessagingConfig {

    @Bean
    @ConditionalOnMissingBean(PesMessageTransport.class)
    public InMemoryPesMessageTransport inMemoryPesMessageTransport() {
        return new InMemoryPesMessageTransport();
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper pesObjectMapper() {
        return JsonMapper.builder().findAndAddModules().build();
    }
}
