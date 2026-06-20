package com.playtogether.pes.wf.query;

import com.playtogether.pes.common.error.PesNotFoundException;
import com.playtogether.pes.common.query.PesHistoryEntry;
import com.playtogether.pes.common.query.PesPage;
import com.playtogether.pes.wf.entity.PesWfHis;
import com.playtogether.pes.wf.entity.PesWfMas;
import com.playtogether.pes.wf.repository.PesWfHisRepository;
import com.playtogether.pes.wf.repository.PesWfMasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * WF 조회(읽기 전용) 서비스.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WfQueryService {

    private final PesWfMasRepository masRepository;
    private final PesWfHisRepository hisRepository;

    public WfView getState(String wfId) {
        PesWfMas mas = this.masRepository.findById(wfId)
                .orElseThrow(() -> new PesNotFoundException("WF", wfId));
        return WfView.from(mas);
    }

    public PesPage<PesHistoryEntry> getHistory(String wfId, String method,
                                              String fromTimekey, String toTimekey,
                                              int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size));
        Page<PesWfHis> result = this.hisRepository.search(wfId, method, fromTimekey, toTimekey, pageable);
        List<PesHistoryEntry> content = result.getContent().stream()
                .map(his -> PesHistoryEntry.of(
                        his.getId().getTimekey(), his.getMethod(), his.getEvent(), his.getMeta()))
                .toList();
        return PesPage.of(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private static int clampSize(int size) {
        if (size < 1) {
            return 20;
        }
        return Math.min(size, 200);
    }
}
