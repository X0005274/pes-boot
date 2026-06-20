package com.playtogether.pes.durable.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * DURABLE workflow step 의 options. Map 위임 + 타입 안전 접근자 방식.
 * 계약 옵션: bindLot(makeInUse), syncToEquipment, syncToHub(공통).
 */
@Schema(description = "DURABLE step 옵션(자유 key/value). 계약 키: bindLot/syncToEquipment/syncToHub(boolean), changeSpec 은 durableSpec(string).",
        example = "{\"bindLot\": true}",
        additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
public record DurableStepOptions(Map<String, Object> values) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public DurableStepOptions {
        values = (values == null) ? Map.of() : Map.copyOf(values);
    }

    public static DurableStepOptions empty() {
        return new DurableStepOptions(Map.of());
    }

    @JsonValue
    @Override
    public Map<String, Object> values() {
        return this.values;
    }

    public boolean bindLot() {
        return boolFlag("bindLot");
    }

    public boolean syncToEquipment() {
        return boolFlag("syncToEquipment");
    }

    public boolean syncToHub() {
        return boolFlag("syncToHub");
    }

    public Object get(String key) {
        return this.values.get(key);
    }

    private boolean boolFlag(String key) {
        Object value = this.values.get(key);
        return (value instanceof Boolean flag) && flag;
    }
}
