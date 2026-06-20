package com.playtogether.pes.lot.entity;

import com.playtogether.pes.common.persistence.PesEventInfoEmbedded;
import com.playtogether.pes.common.persistence.PesMetaEmbedded;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * LOT 이력. workflow step 1건 = HIS row 1건. PK = (LOT_ID, TIMEKEY).
 * EVENT/META 컬럼군은 공통 @Embeddable VO 를 재사용한다.
 */
@Entity
@Table(name = "PES_LOT_HIS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PesLotHis {

    @EmbeddedId
    private PesLotHisId id;

    @Column(name = "METHOD", length = 30, nullable = false)
    private String method;

    @Embedded
    private PesEventInfoEmbedded event;

    @Embedded
    private PesMetaEmbedded meta;
}
