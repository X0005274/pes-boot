package com.playtogether.pes.lot.api;

import java.util.Optional;

/**
 * LOT 도메인에서 허용되는 단일 의미의 method.
 * wire 값(소문자)과 enum 상수를 분리해 직렬화 계약을 안정화한다.
 */
public enum LotMethod {
    CREATED("created"),
    RELEASED("released"),
    CHANGE_SPEC("changeSpec");

    private final String wireName;

    LotMethod(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return this.wireName;
    }

    public static Optional<LotMethod> fromWire(String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (LotMethod method : values()) {
            if (method.wireName.equals(value)) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }
}
