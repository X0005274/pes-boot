package com.playtogether.pes.lot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LOT 이력 복합 PK = LOT_ID + TIMEKEY. TIMEKEY 는 서버에서 생성한다.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PesLotHisId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "LOT_ID", length = 40, nullable = false)
    private String lotId;

    @Column(name = "TIMEKEY", length = 24, nullable = false)
    private String timekey;
}
