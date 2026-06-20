package com.playtogether.pes.common.observability;

import com.playtogether.pes.common.model.EntityType;
import com.playtogether.pes.common.model.PesDomainMessage;
import com.playtogether.pes.common.model.PesMeta;
import org.slf4j.MDC;

/**
 * 처리 컨텍스트를 SLF4J MDC 에 전파한다. 로그 패턴에서 %X{corrId} 등으로 출력 가능.
 * correlationId 는 도메인 간/파생 처리 전반에서 추적의 기준 키가 된다.
 */
public final class PesMdc {

    public static final String CORRELATION_ID = "corrId";
    public static final String ENTITY_TYPE = "entityType";
    public static final String ENTITY_ID = "entityId";
    public static final String SRC_SYSTEM = "srcSystem";

    private PesMdc() {
    }

    public static void populate(PesDomainMessage message) {
        EntityType type = message.resolveEntityType();
        PesMeta meta = message.meta();
        put(ENTITY_TYPE, type != null ? type.name() : null);
        put(ENTITY_ID, message.entityId());
        put(CORRELATION_ID, meta != null ? meta.correlationId() : null);
        put(SRC_SYSTEM, meta != null ? meta.srcSystem() : null);
    }

    public static void clear() {
        MDC.remove(CORRELATION_ID);
        MDC.remove(ENTITY_TYPE);
        MDC.remove(ENTITY_ID);
        MDC.remove(SRC_SYSTEM);
    }

    private static void put(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        } else {
            MDC.remove(key);
        }
    }
}
