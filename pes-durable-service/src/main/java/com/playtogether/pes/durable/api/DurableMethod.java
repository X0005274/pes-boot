package com.playtogether.pes.durable.api;

import java.util.Optional;

/**
 * DURABLE 도메인에서 허용되는 단일 의미의 method. (CLAUDE.md §7)
 */
public enum DurableMethod {
    CREATED("created"),
    MAKE_IN_USE("makeInUse"),
    CHANGE_SPEC("changeSpec");

    private final String wireName;

    DurableMethod(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return this.wireName;
    }

    public static Optional<DurableMethod> fromWire(String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (DurableMethod method : values()) {
            if (method.wireName.equals(value)) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }
}
