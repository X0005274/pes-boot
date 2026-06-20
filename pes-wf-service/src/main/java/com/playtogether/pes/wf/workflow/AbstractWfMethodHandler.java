package com.playtogether.pes.wf.workflow;

import com.playtogether.pes.common.model.PesEventInfo;
import com.playtogether.pes.common.persistence.PesEventInfoEmbedded;
import com.playtogether.pes.common.persistence.PesMetaEmbedded;
import com.playtogether.pes.common.support.PesTimekeyGenerator;
import com.playtogether.pes.wf.api.WfMessage;
import com.playtogether.pes.wf.api.WfWorkflowStep;
import com.playtogether.pes.wf.entity.PesWfHis;
import com.playtogether.pes.wf.entity.PesWfHisId;
import com.playtogether.pes.wf.repository.PesWfHisRepository;
import com.playtogether.pes.wf.repository.PesWfMasRepository;

/**
 * WF method 핸들러 공통 베이스. (LOT 과 동일 패턴)
 */
public abstract class AbstractWfMethodHandler implements WfMethodHandler {

    protected final PesWfMasRepository masRepository;
    protected final PesWfHisRepository hisRepository;
    protected final PesTimekeyGenerator timekeyGenerator;

    protected AbstractWfMethodHandler(PesWfMasRepository masRepository,
                                      PesWfHisRepository hisRepository,
                                      PesTimekeyGenerator timekeyGenerator) {
        this.masRepository = masRepository;
        this.hisRepository = hisRepository;
        this.timekeyGenerator = timekeyGenerator;
    }

    protected PesWfHis appendHistory(WfMessage message, WfWorkflowStep step) {
        String timekey = this.timekeyGenerator.next();
        PesWfHisId id = new PesWfHisId(message.wfId(), timekey);
        PesEventInfoEmbedded event = PesEventInfoEmbedded.from(resolveEvent(step));
        PesMetaEmbedded meta = PesMetaEmbedded.from(message.meta());
        PesWfHis history = new PesWfHis(id, step.method(), event, meta);
        return this.hisRepository.save(history);
    }

    private PesEventInfo resolveEvent(WfWorkflowStep step) {
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
