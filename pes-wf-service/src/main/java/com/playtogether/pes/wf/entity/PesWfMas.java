package com.playtogether.pes.wf.entity;

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
 * WF 마스터. 현재 상태(최신 스냅샷)를 보관한다. PK = WF_ID.
 */
@Entity
@Table(name = "PES_WF_MAS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PesWfMas {

    @Id
    @Column(name = "WF_ID", length = 40, nullable = false)
    private String wfId;

    @Setter
    @Column(name = "LOT_ID", length = 40)
    private String lotId;

    @Setter
    @Column(name = "STAT_TYP", length = 20)
    private String statTyp;

    @Setter
    @Column(name = "WF_SPEC", length = 200)
    private String wfSpec;

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

    public PesWfMas(String wfId) {
        this.wfId = wfId;
    }
}
