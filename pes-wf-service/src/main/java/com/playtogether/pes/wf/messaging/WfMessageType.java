package com.playtogether.pes.wf.messaging;

import com.playtogether.pes.common.messaging.PesMessageType;
import com.playtogether.pes.common.model.EntityType;
import com.playtogether.pes.common.model.PesDomainMessage;
import com.playtogether.pes.wf.api.WfMessage;
import com.playtogether.pes.wf.api.WfSubjects;
import org.springframework.stereotype.Component;

/**
 * WF 도메인 인바운드 메시지 등록. PES.BIZ.WF.EVENT → WfMessage.
 */
@Component
public class WfMessageType implements PesMessageType {

    @Override
    public EntityType entityType() {
        return EntityType.WF;
    }

    @Override
    public String requestSubject() {
        return WfSubjects.UI_REQUEST;
    }

    @Override
    public String inboundSubject() {
        return WfSubjects.BIZ_EVENT;
    }

    @Override
    public String eventSubject() {
        return WfSubjects.UI_EVENT;
    }

    @Override
    public Class<? extends PesDomainMessage> messageClass() {
        return WfMessage.class;
    }
}
