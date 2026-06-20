package com.playtogether.pes.lot.workflow;

import com.playtogether.pes.common.model.PesEventInfo;
import com.playtogether.pes.common.persistence.PesEventInfoEmbedded;
import com.playtogether.pes.common.persistence.PesMetaEmbedded;
import com.playtogether.pes.common.support.PesTimekeyGenerator;
import com.playtogether.pes.lot.api.LotMessage;
import com.playtogether.pes.lot.api.LotWorkflowStep;
import com.playtogether.pes.lot.entity.PesLotHis;
import com.playtogether.pes.lot.entity.PesLotHisId;
import com.playtogether.pes.lot.repository.PesLotHisRepository;
import com.playtogether.pes.lot.repository.PesLotMasRepository;

/**
 * LOT method 핸들러 공통 베이스. 리포지토리/TIMEKEY 생성기 의존성과
 * HIS 적재·event 시각 보정 같은 반복 로직을 모은다.
 */
public abstract class AbstractLotMethodHandler implements LotMethodHandler {

    protected final PesLotMasRepository masRepository;
    protected final PesLotHisRepository hisRepository;
    protected final PesTimekeyGenerator timekeyGenerator;

    protected AbstractLotMethodHandler(PesLotMasRepository masRepository,
                                       PesLotHisRepository hisRepository,
                                       PesTimekeyGenerator timekeyGenerator) {
        this.masRepository = masRepository;
        this.hisRepository = hisRepository;
        this.timekeyGenerator = timekeyGenerator;
    }

    /**
     * step 1건을 PES_LOT_HIS 에 적재한다. TIMEKEY 는 서버 생성,
     * event.eventTm 이 비어 있으면 현재 시각으로 보정한다.
     */
    protected PesLotHis appendHistory(LotMessage message, LotWorkflowStep step) {
        String timekey = this.timekeyGenerator.next();
        PesLotHisId id = new PesLotHisId(message.lotId(), timekey);
        PesEventInfoEmbedded event = PesEventInfoEmbedded.from(resolveEvent(step));
        PesMetaEmbedded meta = PesMetaEmbedded.from(message.meta());
        PesLotHis history = new PesLotHis(id, step.method(), event, meta);
        return this.hisRepository.save(history);
    }

    /** event.eventTm 이 비어 있으면 현재 시각으로 채운 사본을 반환. */
    private PesEventInfo resolveEvent(LotWorkflowStep step) {
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
