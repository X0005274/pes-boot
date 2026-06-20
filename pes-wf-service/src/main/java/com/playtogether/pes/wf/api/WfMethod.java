package com.playtogether.pes.wf.api;

import java.util.Optional;

/**
 * WF 도메인에서 허용되는 단일 의미의 method. (LOT 과 동일 패턴)
 */
public enum WfMethod {
    CREATED("created"),
    RELEASED("released"),
    CHANGE_SPEC("changeSpec");

    private final String wireName;

    WfMethod(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return this.wireName;
    }

    public static Optional<WfMethod> fromWire(String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (WfMethod method : values()) {
            if (method.wireName.equals(value)) {
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }
}
