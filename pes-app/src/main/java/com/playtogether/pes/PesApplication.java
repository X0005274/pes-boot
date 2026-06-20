package com.playtogether.pes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PES 부트스트랩 진입점.
 * base package 가 {@code com.playtogether.pes} 이므로 다음이 자동 스캔된다.
 * <ul>
 *   <li>com.playtogether.pes.common.*  - PesDomainRouter, PesTimekeyGenerator</li>
 *   <li>com.playtogether.pes.lot.*     - LotMessageService, 핸들러, @Entity, Repository</li>
 * </ul>
 * WF/DURABLE 모듈을 추가하면 의존성만 더하면 동일 base package 규칙으로 함께 스캔된다.
 */
@SpringBootApplication
public class PesApplication {

    public static void main(String[] args) {
        SpringApplication.run(PesApplication.class, args);
    }
}
