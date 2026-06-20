package com.playtogether.pes.durable.web;

import com.playtogether.pes.common.query.PesHistoryEntry;
import com.playtogether.pes.common.query.PesPage;
import com.playtogether.pes.durable.query.DurableQueryService;
import com.playtogether.pes.durable.query.DurableView;
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
 * DURABLE 조회 API (읽기 전용).
 */
@RestController
@RequestMapping(path = "/durable", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "DURABLE", description = "DURABLE 조회(상태/이력)")
public class DurableQueryController {

    private final DurableQueryService queryService;

    @GetMapping("/{durableId}")
    @Operation(summary = "DURABLE 현재 상태 조회", description = "PES_DURABLE_MAS 스냅샷. 없으면 404.")
    @ApiResponse(responseCode = "200", description = "현재 상태",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = DurableView.class),
                    examples = @ExampleObject(value = """
                            {
                              "durableId": "DUR001", "lotId": "LOT12345", "statTyp": "IN_USE", "durableSpec": "DSPEC-1",
                              "srcSystem": "UI", "userId": "UIUSER01",
                              "createTm": "20260620153000000", "updateTm": "20260620153003000"
                            }""")))
    @ApiResponse(responseCode = "404", description = "DURABLE 미존재",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public DurableView getState(@PathVariable String durableId) {
        return this.queryService.getState(durableId);
    }

    @GetMapping("/{durableId}/history")
    @Operation(summary = "DURABLE 이력 조회", description = "최신순 페이징. method/from/to 필터 지원.")
    @ApiResponse(responseCode = "200", description = "이력 페이지(최신순)",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "content": [
                                {"timekey": "20260620153003000000", "method": "makeInUse", "eventCd": "INUSE", "statTyp": "IN_USE", "srcSystem": "UI"},
                                {"timekey": "20260620153000000000", "method": "created", "eventCd": "CREATED", "statTyp": "NEW", "srcSystem": "UI"}
                              ],
                              "page": 0, "size": 20, "totalElements": 2, "totalPages": 1
                            }""")))
    public PesPage<PesHistoryEntry> getHistory(
            @PathVariable String durableId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String method,
            @RequestParam(name = "from", required = false) String fromTimekey,
            @RequestParam(name = "to", required = false) String toTimekey) {
        return this.queryService.getHistory(durableId, method, fromTimekey, toTimekey, page, size);
    }
}
