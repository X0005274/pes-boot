package com.playtogether.pes.durable.entity;

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
 * DURABLE 마스터. 현재 상태(최신 스냅샷)를 보관한다. PK = DURABLE_ID.
 */
@Entity
@Table(name = "PES_DURABLE_MAS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PesDurableMas {

    @Id
    @Column(name = "DURABLE_ID", length = 40, nullable = false)
    private String durableId;

    @Setter
    @Column(name = "LOT_ID", length = 40)
    private String lotId;

    @Setter
    @Column(name = "STAT_TYP", length = 20)
    private String statTyp;

    @Setter
    @Column(name = "DURABLE_SPEC", length = 200)
    private String durableSpec;

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

    @Version
    @Column(name = "VER_NO")
    private Long verNo;

    public PesDurableMas(String durableId) {
        this.durableId = durableId;
    }
}
