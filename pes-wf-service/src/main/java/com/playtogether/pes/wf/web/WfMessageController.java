package com.playtogether.pes.wf.web;

import com.playtogether.pes.common.workflow.PesMessageProcessor;
import com.playtogether.pes.common.workflow.PesProcessResult;
import com.playtogether.pes.wf.api.WfMessage;
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
 * WF 메시지 임시 진입점. (LOT 과 동일 패턴, 공통 PesApiExceptionHandler 적용)
 */
@RestController
@RequestMapping(path = "/wf", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "WF", description = "WF 명령(workflow 처리)")
public class WfMessageController {

    private final PesMessageProcessor processor;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "WF 메시지 처리", description = "workflow 배열을 순차 처리. inheritLotSpec/applyToLot 등 LOT 연계 옵션 지원.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 결과(SUCCESS/FAILED)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PesProcessResult.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "entityType": "WF", "entityId": "WF56789", "correlationId": "TX-1",
                                      "status": "SUCCESS",
                                      "steps": [{"method": "created", "status": "SUCCESS", "message": "WF created: wfId=WF56789, lotId=LOT12345"}]
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "검증/계약 위반(entityType≠WF 등)",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "501", description = "WF 도메인 핸들러 미등록(모듈 미배포 등)",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PesProcessResult handle(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "created(inheritLotSpec)", value = """
                            {
                              "entityType": "WF", "wfId": "WF56789", "lotId": "LOT12345",
                              "workflow": [{"method": "created", "options": {"inheritLotSpec": true}, "event": {"eventCd": "CREATED", "statTyp": "NEW"}}],
                              "meta": {"srcSystem": "UI"}
                            }""")))
            @Valid @RequestBody WfMessage message) {
        return this.processor.process(message);
    }
}
