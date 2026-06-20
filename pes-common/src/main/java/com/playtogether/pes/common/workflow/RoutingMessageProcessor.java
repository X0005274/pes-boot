package com.playtogether.pes.common.workflow;

import com.playtogether.pes.common.model.PesDomainMessage;
import com.playtogether.pes.common.observability.PesCorrelationIds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 기본 처리기. 멱등 처리 없이 라우터로 위임한다.
 * idempotency 모듈이 @Primary 구현을 제공하면 그쪽이 우선 주입된다.
 */
@Component
@RequiredArgsConstructor
public class RoutingMessageProcessor implements PesMessageProcessor {

    private final PesDomainRouter router;

    @Override
    public PesProcessResult process(PesDomainMessage message) {
        return this.router.route(PesCorrelationIds.ensure(message));
    }
}
