package com.playtogether.pes.lot.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * LOT workflow step 의 options.
 * 내부적으로 임의 key/value Map 을 보유하되, 계약된 옵션은 타입 안전 접근자로 노출한다.
 * JSON object 전체가 그대로 values 에 매핑된다(delegating). 새 옵션 추가 시 접근자만 늘리면 된다.
 */
@Schema(description = "LOT step 옵션(자유 key/value). 계약 키: createWf/createDurable/makeDurableInUse/syncToHub(boolean), changeSpec 은 lotSpec(string).",
        example = "{\"createWf\": true, \"createDurable\": true}",
        additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
public record LotStepOptions(Map<String, Object> values) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public LotStepOptions {
        values = (values == null) ? Map.of() : Map.copyOf(values);
    }

    public static LotStepOptions empty() {
        return new LotStepOptions(Map.of());
    }

    /** 직렬화 시 wrapper 없이 원래 map 형태로 출력. */
    @JsonValue
    @Override
    public Map<String, Object> values() {
        return this.values;
    }

    // ── 계약된 옵션 (boolean) ──
    public boolean createWf() {
        return boolFlag("createWf");
    }

    public boolean createDurable() {
        return boolFlag("createDurable");
    }

    public boolean makeDurableInUse() {
        return boolFlag("makeDurableInUse");
    }

    public boolean syncToHub() {
        return boolFlag("syncToHub");
    }

    /** 계약 외 옵션 raw 조회 (예: changeSpec 의 lotSpec). */
    public Object get(String key) {
        return this.values.get(key);
    }

    private boolean boolFlag(String key) {
        Object value = this.values.get(key);
        return (value instanceof Boolean flag) && flag;
    }
}
