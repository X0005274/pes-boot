package com.playtogether.pes.lot.workflow;

import com.playtogether.pes.common.workflow.PesStepResult;
import com.playtogether.pes.lot.api.LotMessage;
import com.playtogether.pes.lot.api.LotMethod;
import com.playtogether.pes.lot.api.LotWorkflowStep;

/**
 * LOT method 단위 처리 전략. method 마다 한 구현을 둔다(created/released/changeSpec).
 * 새 method 추가 시 이 인터페이스 구현 빈만 추가하면 {@link LotWorkflowHandler} 가 자동 등록한다.
 */
public interface LotMethodHandler {

    /** 이 핸들러가 담당하는 method. */
    LotMethod method();

    /** 단일 step 을 처리한다. (LOT_MAS 갱신, LOT_HIS 적재, 연계 옵션 처리 등) */
    PesStepResult handle(LotMessage message, LotWorkflowStep step);
}
