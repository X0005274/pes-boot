package com.playtogether.pes.wf.entity;

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
 * WF 이력. workflow step 1건 = HIS row 1건. PK = (WF_ID, TIMEKEY).
 */
@Entity
@Table(name = "PES_WF_HIS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PesWfHis {

    @EmbeddedId
    private PesWfHisId id;

    @Column(name = "METHOD", length = 30, nullable = false)
    private String method;

    @Embedded
    private PesEventInfoEmbedded event;

    @Embedded
    private PesMetaEmbedded meta;
}
