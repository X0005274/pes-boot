package com.playtogether.pes.common.workflow;

import com.playtogether.pes.common.model.EntityType;
import com.playtogether.pes.common.model.PesDomainMessage;
import com.playtogether.pes.common.observability.PesMdc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * entityType 기준으로 도메인 핸들러에 메시지를 라우팅한다.
 * Spring 컨텍스트에 등록된 모든 {@link PesDomainHandler} 빈을 EntityType 키로 모은다.
 *
 * <p>현재는 LOT 핸들러만 등록된다. WF/DURABLE 은 각 도메인 서비스가
 * {@code PesDomainHandler} 빈을 추가하면 코드 변경 없이 자동으로 라우팅 대상에 포함된다.
 */
@Component
public class PesDomainRouter {

    private static final Logger log = LoggerFactory.getLogger(PesDomainRouter.class);

    private final Map<EntityType, PesDomainHandler<? extends PesDomainMessage>> handlers;

    public PesDomainRouter(List<PesDomainHandler<? extends PesDomainMessage>> handlerBeans) {
        Map<EntityType, PesDomainHandler<? extends PesDomainMessage>> map =
                new EnumMap<>(EntityType.class);
        for (PesDomainHandler<? extends PesDomainMessage> handler : handlerBeans) {
            PesDomainHandler<? extends PesDomainMessage> previous =
                    map.put(handler.entityType(), handler);
            if (previous != null) {
                throw new IllegalStateException(
                        "동일 EntityType 에 핸들러가 중복 등록되었습니다: " + handler.entityType());
            }
        }
        this.handlers = Collections.unmodifiableMap(map);
    }

    @SuppressWarnings("unchecked")
    public PesProcessResult route(PesDomainMessage message) {
        EntityType type = message.resolveEntityType();
        PesDomainHandler<PesDomainMessage> handler =
                (PesDomainHandler<PesDomainMessage>) this.handlers.get(type);
        if (handler == null) {
            throw new UnsupportedOperationException(
                    "처리할 수 있는 도메인 핸들러가 없습니다: " + type
                            + " (등록된 도메인=" + this.handlers.keySet() + ")");
        }

        // 처리 컨텍스트(correlationId 등)를 MDC 로 전파 → 처리 중 모든 로그에 상관관계 ID 포함
        PesMdc.populate(message);
        try {
            log.debug("routing entityType={} entityId={}", type, message.entityId());
            return handler.handle(message);
        } finally {
            PesMdc.clear();
        }
    }
}
