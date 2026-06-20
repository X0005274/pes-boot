package com.playtogether.pes.lot.web;

import com.playtogether.pes.common.workflow.PesMessageProcessor;
import com.playtogether.pes.common.workflow.PesProcessResult;
import com.playtogether.pes.lot.api.LotMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LOT 메시지 임시 진입점. RV 어댑터 도입 전, end-to-end 흐름 확인용 REST 엔드포인트.
 * UI → (이 컨트롤러) → PesDomainRouter → LotMessageService → workflow 처리.
 */
@RestController
@RequestMapping(path = "/lot", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "LOT", description = "LOT 명령(workflow 처리)")
public class LotMessageController {

    private final PesMessageProcessor processor;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "LOT 메시지 처리",
            description = "workflow 배열을 순차 처리한다. 실패해도 200 으로 PesProcessResult 를 반환(status 로 구분).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 결과(SUCCESS/FAILED)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PesProcessResult.class),
                            examples = @ExampleObject(name = "success", value = """
                                    {
                                      "entityType": "LOT",
                                      "entityId": "LOT12345",
                                      "correlationId": "TX-20260620-0001",
                                      "status": "SUCCESS",
                                      "steps": [
                                        {"method": "created", "status": "SUCCESS", "message": "LOT created: lotId=LOT12345"},
                                        {"method": "released", "status": "SUCCESS", "message": "LOT released: lotId=LOT12345, durableId=DUR001"}
                                      ]
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "검증/계약 위반(entityType≠LOT, workflow 비어있음 등)",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "501", description = "LOT 도메인 핸들러 미등록(모듈 미배포 등)",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PesProcessResult handle(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "created+released", value = """
                            {
                              "entityType": "LOT",
                              "lotId": "LOT12345",
                              "wfId": "WF56789",
                              "durableId": "DUR001",
                              "workflow": [
                                {"method": "created", "options": {"createWf": true}, "event": {"eventCd": "CREATED", "statTyp": "NEW"}},
                                {"method": "released", "options": {"makeDurableInUse": true}, "event": {"eventCd": "RELEASED", "statTyp": "REL"}}
                              ],
                              "meta": {"srcSystem": "UI", "userId": "UIUSER01", "correlationId": "TX-20260620-0001"}
                            }""")))
            @Valid @RequestBody LotMessage message) {
        return this.processor.process(message);
    }
}
