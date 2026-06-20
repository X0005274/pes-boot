package com.playtogether.pes.common.observability;

import com.playtogether.pes.common.model.PesDomainMessage;
import com.playtogether.pes.common.model.PesMeta;

import java.util.UUID;

/**
 * correlationId 발급/보정 유틸. 인바운드 경계에서 correlationId 가 없으면 서버가 발급해
 * 멱등성·추적·응답에 일관되게 흐르도록 한다.
 */
public final class PesCorrelationIds {

    private static final String PREFIX = "PES-";

    private PesCorrelationIds() {
    }

    public static String generate() {
        return PREFIX + UUID.randomUUID();
    }

    /** correlationId 가 비어 있으면 서버 발급 값으로 채운 메시지를 반환한다. */
    public static PesDomainMessage ensure(PesDomainMessage message) {
        PesMeta meta = message.meta();
        if (meta != null && meta.correlationId() != null && !meta.correlationId().isBlank()) {
            return message;
        }
        return message.withCorrelationId(generate());
    }
}
