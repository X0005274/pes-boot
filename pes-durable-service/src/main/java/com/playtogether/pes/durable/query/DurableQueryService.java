package com.playtogether.pes.durable.query;

import com.playtogether.pes.common.error.PesNotFoundException;
import com.playtogether.pes.common.query.PesHistoryEntry;
import com.playtogether.pes.common.query.PesPage;
import com.playtogether.pes.durable.entity.PesDurableHis;
import com.playtogether.pes.durable.entity.PesDurableMas;
import com.playtogether.pes.durable.repository.PesDurableHisRepository;
import com.playtogether.pes.durable.repository.PesDurableMasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DURABLE 조회(읽기 전용) 서비스.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DurableQueryService {

    private final PesDurableMasRepository masRepository;
    private final PesDurableHisRepository hisRepository;

    public DurableView getState(String durableId) {
        PesDurableMas mas = this.masRepository.findById(durableId)
                .orElseThrow(() -> new PesNotFoundException("DURABLE", durableId));
        return DurableView.from(mas);
    }

    public PesPage<PesHistoryEntry> getHistory(String durableId, String method,
                                              String fromTimekey, String toTimekey,
                                              int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size));
        Page<PesDurableHis> result = this.hisRepository.search(durableId, method, fromTimekey, toTimekey, pageable);
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
