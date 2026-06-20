package com.playtogether.pes.wf.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * WF workflow step 의 options. LOT 과 동일하게 Map 위임 + 타입 안전 접근자 방식.
 * 계약 옵션: inheritLotSpec(created), applyToLot(changeSpec), syncToHub(공통).
 */
@Schema(description = "WF step 옵션(자유 key/value). 계약 키: inheritLotSpec/applyToLot/syncToHub(boolean), changeSpec 은 wfSpec(string).",
        example = "{\"inheritLotSpec\": true}",
        additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
public record WfStepOptions(Map<String, Object> values) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public WfStepOptions {
        values = (values == null) ? Map.of() : Map.copyOf(values);
    }

    public static WfStepOptions empty() {
        return new WfStepOptions(Map.of());
    }

    @JsonValue
    @Override
    public Map<String, Object> values() {
        return this.values;
    }

    public boolean inheritLotSpec() {
        return boolFlag("inheritLotSpec");
    }

    public boolean applyToLot() {
        return boolFlag("applyToLot");
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
