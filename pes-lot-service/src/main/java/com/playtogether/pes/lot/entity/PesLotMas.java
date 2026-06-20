package com.playtogether.pes.lot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * LOT 마스터. 현재 상태(최신 스냅샷)를 보관한다. PK = LOT_ID.
 * 이력은 {@link PesLotHis} 에 별도 적재한다.
 */
@Entity
@Table(name = "PES_LOT_MAS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PesLotMas {

    @Id
    @Column(name = "LOT_ID", length = 40, nullable = false)
    private String lotId;

    @Setter
    @Column(name = "WF_ID", length = 40)
    private String wfId;

    @Setter
    @Column(name = "DURABLE_ID", length = 40)
    private String durableId;

    @Setter
    @Column(name = "STAT_TYP", length = 20)
    private String statTyp;

    @Setter
    @Column(name = "LOT_SPEC", length = 200)
    private String lotSpec;

    @Setter
    @Column(name = "SRC_SYSTEM", length = 20)
    private String srcSystem;

    @Setter
    @Column(name = "USER_ID", length = 30)
    private String userId;

    @Setter
    @Column(name = "CREATE_TM", length = 20)
    private String createTm;

    @Setter
    @Column(name = "UPDATE_TM", length = 20)
    private String updateTm;

    /** 낙관적 잠금용 버전. */
    @Version
    @Column(name = "VER_NO")
    private Long verNo;

    public PesLotMas(String lotId) {
        this.lotId = lotId;
    }
}
