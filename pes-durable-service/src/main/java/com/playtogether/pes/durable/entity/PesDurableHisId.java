package com.playtogether.pes.durable.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DURABLE 이력 복합 PK = DURABLE_ID + TIMEKEY. TIMEKEY 는 서버에서 생성한다.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PesDurableHisId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "DURABLE_ID", length = 40, nullable = false)
    private String durableId;

    @Column(name = "TIMEKEY", length = 24, nullable = false)
    private String timekey;
}
