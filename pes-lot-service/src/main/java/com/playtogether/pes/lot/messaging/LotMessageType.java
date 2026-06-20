package com.playtogether.pes.lot.messaging;

import com.playtogether.pes.common.messaging.PesMessageType;
import com.playtogether.pes.common.model.EntityType;
import com.playtogether.pes.common.model.PesDomainMessage;
import com.playtogether.pes.lot.api.LotMessage;
import com.playtogether.pes.lot.api.LotSubjects;
import org.springframework.stereotype.Component;

/**
 * LOT 도메인 인바운드 메시지 등록. PES.BIZ.LOT.EVENT → LotMessage.
 */
@Component
public class LotMessageType implements PesMessageType {

    @Override
    public EntityType entityType() {
        return EntityType.LOT;
    }

    @Override
    public String requestSubject() {
        return LotSubjects.UI_REQUEST;
    }

    @Override
    public String inboundSubject() {
        return LotSubjects.BIZ_EVENT;
    }

    @Override
    public String eventSubject() {
        return LotSubjects.UI_EVENT;
    }

    @Override
    public Class<? extends PesDomainMessage> messageClass() {
        return LotMessage.class;
    }
}
