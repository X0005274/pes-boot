package com.playtogether.pes.durable.workflow;

import com.playtogether.pes.common.model.PesEventInfo;
import com.playtogether.pes.common.persistence.PesEventInfoEmbedded;
import com.playtogether.pes.common.persistence.PesMetaEmbedded;
import com.playtogether.pes.common.support.PesTimekeyGenerator;
import com.playtogether.pes.durable.api.DurableMessage;
import com.playtogether.pes.durable.api.DurableWorkflowStep;
import com.playtogether.pes.durable.entity.PesDurableHis;
import com.playtogether.pes.durable.entity.PesDurableHisId;
import com.playtogether.pes.durable.repository.PesDurableHisRepository;
import com.playtogether.pes.durable.repository.PesDurableMasRepository;

/**
 * DURABLE method 핸들러 공통 베이스. (LOT/WF 와 동일 패턴)
 */
public abstract class AbstractDurableMethodHandler implements DurableMethodHandler {

    protected final PesDurableMasRepository masRepository;
    protected final PesDurableHisRepository hisRepository;
    protected final PesTimekeyGenerator timekeyGenerator;

    protected AbstractDurableMethodHandler(PesDurableMasRepository masRepository,
                                           PesDurableHisRepository hisRepository,
                                           PesTimekeyGenerator timekeyGenerator) {
        this.masRepository = masRepository;
        this.hisRepository = hisRepository;
        this.timekeyGenerator = timekeyGenerator;
    }

    protected PesDurableHis appendHistory(DurableMessage message, DurableWorkflowStep step) {
        String timekey = this.timekeyGenerator.next();
        PesDurableHisId id = new PesDurableHisId(message.durableId(), timekey);
        PesEventInfoEmbedded event = PesEventInfoEmbedded.from(resolveEvent(step));
        PesMetaEmbedded meta = PesMetaEmbedded.from(message.meta());
        PesDurableHis history = new PesDurableHis(id, step.method(), event, meta);
        return this.hisRepository.save(history);
    }

    private PesEventInfo resolveEvent(DurableWorkflowStep step) {
        PesEventInfo event = step.event();
        if (event == null) {
            return null;
        }
        if (event.eventTm() != null && !event.eventTm().isBlank()) {
            return event;
        }
        return new PesEventInfo(
                event.eventCd(),
                this.timekeyGenerator.currentTimestamp(),
                event.eventDesc(),
                event.statTyp());
    }
}
