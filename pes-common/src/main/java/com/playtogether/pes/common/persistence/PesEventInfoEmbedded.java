package com.playtogether.pes.common.persistence;

import com.playtogether.pes.common.model.PesEventInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@link PesEventInfo} 의 영속(@Embeddable) 버전. *_HIS 테이블의 EVENT 컬럼군에 매핑.
 * LOT/WF/DURABLE HIS 엔티티에서 공통 재사용한다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PesEventInfoEmbedded {

    @Column(name = "EVENT_CD", length = 30, nullable = false)
    private String eventCd;

    @Column(name = "EVENT_TM", length = 20)
    private String eventTm;

    @Column(name = "EVENT_DESC", length = 200)
    private String eventDesc;

    @Column(name = "STAT_TYP", length = 20)
    private String statTyp;

    public static PesEventInfoEmbedded from(PesEventInfo info) {
        if (info == null) {
            return null;
        }
        return new PesEventInfoEmbedded(
                info.eventCd(), info.eventTm(), info.eventDesc(), info.statTyp());
    }

    public PesEventInfo toModel() {
        return new PesEventInfo(this.eventCd, this.eventTm, this.eventDesc, this.statTyp);
    }
}
