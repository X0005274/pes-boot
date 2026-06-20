package com.playtogether.pes.durable.entity;

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
 * DURABLE 이력. workflow step 1건 = HIS row 1건. PK = (DURABLE_ID, TIMEKEY).
 */
@Entity
@Table(name = "PES_DURABLE_HIS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PesDurableHis {

    @EmbeddedId
    private PesDurableHisId id;

    @Column(name = "METHOD", length = 30, nullable = false)
    private String method;

    @Embedded
    private PesEventInfoEmbedded event;

    @Embedded
    private PesMetaEmbedded meta;
}
