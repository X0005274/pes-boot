package com.playtogether.pes.messaging;

import com.playtogether.pes.common.model.PesEventInfo;
import com.playtogether.pes.common.model.PesMeta;
import com.playtogether.pes.lot.api.LotMessage;
import com.playtogether.pes.lot.api.LotStepOptions;
import com.playtogether.pes.lot.api.LotWorkflowStep;
import com.playtogether.pes.lot.repository.PesLotMasRepository;
import com.playtogether.pes.lot.service.LotMessageService;
import com.playtogether.pes.messaging.transport.InMemoryPesMessageTransport;
import com.playtogether.pes.messaging.transport.PesMessageTransport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * afterCommit 발행 정합 검증: 롤백 시 이벤트 미발행, 커밋 시 발행.
 */
@SpringBootTest
class EventAfterCommitIntegrationTest {

    @Autowired
    private PesMessageTransport transport;

    @Autowired
    private LotMessageService lotMessageService;

    @Autowired
    private PesLotMasRepository lotMasRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private InMemoryPesMessageTransport memory() {
        return (InMemoryPesMessageTransport) this.transport;
    }

    private LotMessage createdMessage(String lotId) {
        LotWorkflowStep step = new LotWorkflowStep(
                "created",
                LotStepOptions.empty(),
                new PesEventInfo("CREATED", null, null, "NEW"));
        return new LotMessage("LOT", lotId, null, null, List.of(step),
                new PesMeta("UI", null, null, null, null));
    }

    @Test
    void rollback_suppressesEventPublish() {
        String lotId = "LOT-TX-RB";
        int before = memory().publishedEvents().size();
        TransactionTemplate tx = new TransactionTemplate(this.transactionManager);

        tx.executeWithoutResult(status -> {
            this.lotMessageService.handle(createdMessage(lotId));  // 트랜잭션 참여 + afterCommit 등록
            status.setRollbackOnly();                              // 강제 롤백
        });

        // 롤백 → 이벤트 미발행 + DB 미반영
        assertThat(memory().publishedEvents())
                .noneMatch(event -> event.payload().contains(lotId));
        assertThat(memory().publishedEvents()).hasSize(before);
        assertThat(this.lotMasRepository.findById(lotId)).isEmpty();
    }

    @Test
    void commit_publishesEventAfterCommit() {
        String lotId = "LOT-TX-OK";
        TransactionTemplate tx = new TransactionTemplate(this.transactionManager);

        tx.executeWithoutResult(status ->
                this.lotMessageService.handle(createdMessage(lotId)));

        // 커밋 → 이벤트 발행 + DB 반영
        assertThat(memory().publishedEvents())
                .anyMatch(event -> event.subject().equals("PES.UI.LOT.EVENT")
                        && event.payload().contains(lotId));
        assertThat(this.lotMasRepository.findById(lotId)).isPresent();
    }
}
