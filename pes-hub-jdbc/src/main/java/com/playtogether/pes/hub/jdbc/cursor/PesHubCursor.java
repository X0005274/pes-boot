package com.playtogether.pes.hub.jdbc.cursor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Hub 적재 워터마크 커서. PES DB(기본 DataSource)에 영속되어 재시작 후 증분을 이어간다.
 * PK = 커서 키(예: "his:LOT", "his:WF", "his:DURABLE").
 */
@Entity
@Table(name = "PES_HUB_CURSOR")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PesHubCursor {

    @Id
    @Column(name = "CURSOR_KEY", length = 60, nullable = false)
    private String cursorKey;

    @Setter
    @Column(name = "WATERMARK", length = 40)
    private String watermark;

    @Setter
    @Column(name = "UPDATE_TM", length = 20)
    private String updateTm;

    public PesHubCursor(String cursorKey) {
        this.cursorKey = cursorKey;
    }
}
