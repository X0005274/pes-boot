package com.playtogether.pes.idempotency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 처리 완료된 메시지의 멱등 마커. PK = correlationId.
 * 성공 처리 결과(JSON)를 보관해, 중복 수신 시 재처리 없이 동일 결과를 반환한다.
 */
@Entity
@Table(name = "PES_PROCESSED_MSG")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PesProcessedMessage {

    @Id
    @Column(name = "CORR_ID", length = 60, nullable = false)
    private String correlationId;

    @Column(name = "ENTITY_TYPE", length = 20)
    private String entityType;

    @Column(name = "ENTITY_ID", length = 40)
    private String entityId;

    @Column(name = "STAT_TYP", length = 20)
    private String status;

    @Lob
    @Column(name = "RESULT_JSON")
    private String resultJson;

    @Column(name = "PROCESSED_TM", length = 20)
    private String processedTm;
}
