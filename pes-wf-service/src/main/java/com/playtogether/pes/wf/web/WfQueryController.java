package com.playtogether.pes.wf.web;

import com.playtogether.pes.common.query.PesHistoryEntry;
import com.playtogether.pes.common.query.PesPage;
import com.playtogether.pes.wf.query.WfQueryService;
import com.playtogether.pes.wf.query.WfView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * WF 조회 API (읽기 전용).
 */
@RestController
@RequestMapping(path = "/wf", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "WF", description = "WF 조회(상태/이력)")
public class WfQueryController {

    private final WfQueryService queryService;

    @GetMapping("/{wfId}")
    @Operation(summary = "WF 현재 상태 조회", description = "PES_WF_MAS 스냅샷. 없으면 404.")
    @ApiResponse(responseCode = "200", description = "현재 상태",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WfView.class),
                    examples = @ExampleObject(value = """
                            {
                              "wfId": "WF56789", "lotId": "LOT12345", "statTyp": "NEW", "wfSpec": "WSPEC-1",
                              "srcSystem": "UI", "userId": "UIUSER01",
                              "createTm": "20260620153000000", "updateTm": "20260620153002000"
                            }""")))
    @ApiResponse(responseCode = "404", description = "WF 미존재",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public WfView getState(@PathVariable String wfId) {
        return this.queryService.getState(wfId);
    }

    @GetMapping("/{wfId}/history")
    @Operation(summary = "WF 이력 조회", description = "최신순 페이징. method/from/to 필터 지원.")
    @ApiResponse(responseCode = "200", description = "이력 페이지(최신순)",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "content": [
                                {"timekey": "20260620153002000000", "method": "changeSpec", "eventCd": "CHG", "statTyp": "NEW", "srcSystem": "UI", "correlationId": "TX-1"},
                                {"timekey": "20260620153000000000", "method": "created", "eventCd": "CREATED", "statTyp": "NEW", "srcSystem": "UI", "correlationId": "TX-1"}
                              ],
                              "page": 0, "size": 20, "totalElements": 2, "totalPages": 1
                            }""")))
    public PesPage<PesHistoryEntry> getHistory(
            @PathVariable String wfId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String method,
            @RequestParam(name = "from", required = false) String fromTimekey,
            @RequestParam(name = "to", required = false) String toTimekey) {
        return this.queryService.getHistory(wfId, method, fromTimekey, toTimekey, page, size);
    }
}
