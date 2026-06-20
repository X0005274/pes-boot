package com.playtogether.pes.common.persistence;

import com.playtogether.pes.common.model.PesMeta;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@link PesMeta} 의 영속(@Embeddable) 버전. *_HIS 테이블의 audit 컬럼군에 매핑.
 * LOT/WF/DURABLE HIS 엔티티에서 공통 재사용한다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PesMetaEmbedded {

    @Column(name = "SRC_SYSTEM", length = 20, nullable = false)
    private String srcSystem;

    @Column(name = "USER_ID", length = 30)
    private String userId;

    @Column(name = "CORR_ID", length = 60)
    private String correlationId;

    @Column(name = "REQUEST_TM", length = 20)
    private String requestTm;

    @Column(name = "LOCALE", length = 20)
    private String locale;

    public static PesMetaEmbedded from(PesMeta meta) {
        if (meta == null) {
            return null;
        }
        return new PesMetaEmbedded(
                meta.srcSystem(), meta.userId(), meta.correlationId(),
                meta.requestTm(), meta.locale());
    }

    public PesMeta toModel() {
        return new PesMeta(this.srcSystem, this.userId, this.correlationId,
                this.requestTm, this.locale);
    }
}
