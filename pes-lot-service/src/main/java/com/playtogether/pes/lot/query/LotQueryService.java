package com.playtogether.pes.lot.query;

import com.playtogether.pes.common.error.PesNotFoundException;
import com.playtogether.pes.common.query.PesHistoryEntry;
import com.playtogether.pes.common.query.PesPage;
import com.playtogether.pes.lot.entity.PesLotHis;
import com.playtogether.pes.lot.entity.PesLotMas;
import com.playtogether.pes.lot.repository.PesLotHisRepository;
import com.playtogether.pes.lot.repository.PesLotMasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * LOT 조회(읽기 전용) 서비스.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LotQueryService {

    private final PesLotMasRepository masRepository;
    private final PesLotHisRepository hisRepository;

    public LotView getState(String lotId) {
        PesLotMas mas = this.masRepository.findById(lotId)
                .orElseThrow(() -> new PesNotFoundException("LOT", lotId));
        return LotView.from(mas);
    }

    public PesPage<PesHistoryEntry> getHistory(String lotId, String method,
                                               String fromTimekey, String toTimekey,
                                               int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size));
        Page<PesLotHis> result = this.hisRepository.search(lotId, method, fromTimekey, toTimekey, pageable);
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
