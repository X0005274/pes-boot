package com.playtogether.pes.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.playtogether.pes.common.model.PesEventInfo;
import com.playtogether.pes.common.model.PesMeta;
import com.playtogether.pes.common.observability.PesMdc;
import com.playtogether.pes.common.workflow.PesDomainRouter;
import com.playtogether.pes.lot.api.LotMessage;
import com.playtogether.pes.lot.api.LotStepOptions;
import com.playtogether.pes.lot.api.LotWorkflowStep;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관측성 검증: 처리 중 correlationId 등이 MDC 로 전파되고, 처리 후 정리되는지 확인.
 */
@SpringBootTest
class ObservabilityIntegrationTest {

    @Autowired
    private PesDomainRouter router;

    private final Logger routerLogger = (Logger) LoggerFactory.getLogger(PesDomainRouter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @AfterEach
    void tearDown() {
        this.routerLogger.detachAppender(this.appender);
    }

    private LotMessage createdMessage(String lotId, String correlationId) {
        LotWorkflowStep step = new LotWorkflowStep(
                "created",
                LotStepOptions.empty(),
                new PesEventInfo("CREATED", null, null, "NEW"));
        return new LotMessage("LOT", lotId, null, null, List.of(step),
                new PesMeta("UI", "U1", correlationId, null, null));
    }

    @Test
    void correlationId_isPropagatedToMdc_duringRouting_andClearedAfter() {
        this.appender.start();
        this.routerLogger.setLevel(Level.DEBUG);
        this.routerLogger.addAppender(this.appender);

        this.router.route(createdMessage("LOT-OBS-1", "TX-OBS-1"));

        // 처리 중 로그 이벤트의 MDC 에 correlationId/entityId 가 들어있어야 함
        assertThat(this.appender.list)
                .anyMatch(event -> "TX-OBS-1".equals(event.getMDCPropertyMap().get(PesMdc.CORRELATION_ID))
                        && "LOT-OBS-1".equals(event.getMDCPropertyMap().get(PesMdc.ENTITY_ID))
                        && "LOT".equals(event.getMDCPropertyMap().get(PesMdc.ENTITY_TYPE)));

        // 처리 후 MDC 는 정리되어야 함(누수 방지)
        assertThat(MDC.get(PesMdc.CORRELATION_ID)).isNull();
        assertThat(MDC.get(PesMdc.ENTITY_ID)).isNull();
    }
}
