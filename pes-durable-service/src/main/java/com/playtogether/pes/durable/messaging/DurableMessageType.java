package com.playtogether.pes.durable.messaging;

import com.playtogether.pes.common.messaging.PesMessageType;
import com.playtogether.pes.common.model.EntityType;
import com.playtogether.pes.common.model.PesDomainMessage;
import com.playtogether.pes.durable.api.DurableMessage;
import com.playtogether.pes.durable.api.DurableSubjects;
import org.springframework.stereotype.Component;

/**
 * DURABLE 도메인 인바운드 메시지 등록. PES.BIZ.DURABLE.EVENT → DurableMessage.
 */
@Component
public class DurableMessageType implements PesMessageType {

    @Override
    public EntityType entityType() {
        return EntityType.DURABLE;
    }

    @Override
    public String requestSubject() {
        return DurableSubjects.UI_REQUEST;
    }

    @Override
    public String inboundSubject() {
        return DurableSubjects.BIZ_EVENT;
    }

    @Override
    public String eventSubject() {
        return DurableSubjects.UI_EVENT;
    }

    @Override
    public Class<? extends PesDomainMessage> messageClass() {
        return DurableMessage.class;
    }
}
