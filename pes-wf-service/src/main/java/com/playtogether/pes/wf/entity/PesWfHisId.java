package com.playtogether.pes.wf.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * WF 이력 복합 PK = WF_ID + TIMEKEY. TIMEKEY 는 서버에서 생성한다.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PesWfHisId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "WF_ID", length = 40, nullable = false)
    private String wfId;

    @Column(name = "TIMEKEY", length = 24, nullable = false)
    private String timekey;
}
