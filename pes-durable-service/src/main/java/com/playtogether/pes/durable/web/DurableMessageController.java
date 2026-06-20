package com.playtogether.pes.durable.web;

import com.playtogether.pes.common.workflow.PesMessageProcessor;
import com.playtogether.pes.common.workflow.PesProcessResult;
import com.playtogether.pes.durable.api.DurableMessage;
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
 * DURABLE 메시지 임시 진입점. (LOT/WF 와 동일 패턴, 공통 PesApiExceptionHandler 적용)
 */
@RestController
@RequestMapping(path = "/durable", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "DURABLE", description = "DURABLE 명령(workflow 처리)")
public class DurableMessageController {

    private final PesMessageProcessor processor;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "DURABLE 메시지 처리", description = "workflow 배열을 순차 처리. makeInUse/bindLot 등 지원.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 결과(SUCCESS/FAILED)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PesProcessResult.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "entityType": "DURABLE", "entityId": "DUR001", "correlationId": "TX-1",
                                      "status": "SUCCESS",
                                      "steps": [
                                        {"method": "created", "status": "SUCCESS", "message": "DURABLE created: durableId=DUR001"},
                                        {"method": "makeInUse", "status": "SUCCESS", "message": "DURABLE makeInUse: durableId=DUR001, lotId=LOT12345"}
                                      ]
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "검증/계약 위반(entityType≠DURABLE 등)",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "501", description = "DURABLE 도메인 핸들러 미등록(모듈 미배포 등)",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PesProcessResult handle(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(name = "created+makeInUse(bindLot)", value = """
                            {
                              "entityType": "DURABLE", "durableId": "DUR001", "lotId": "LOT12345",
                              "workflow": [
                                {"method": "created", "options": {}, "event": {"eventCd": "CREATED", "statTyp": "NEW"}},
                                {"method": "makeInUse", "options": {"bindLot": true}, "event": {"eventCd": "INUSE", "statTyp": "IN_USE"}}
                              ],
                              "meta": {"srcSystem": "UI"}
                            }""")))
            @Valid @RequestBody DurableMessage message) {
        return this.processor.process(message);
    }
}
