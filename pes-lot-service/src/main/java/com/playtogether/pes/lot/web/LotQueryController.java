package com.playtogether.pes.lot.web;

import com.playtogether.pes.common.query.PesHistoryEntry;
import com.playtogether.pes.common.query.PesPage;
import com.playtogether.pes.lot.query.LotQueryService;
import com.playtogether.pes.lot.query.LotView;
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
 * LOT 조회 API (읽기 전용). 상태/이력 조회.
 */
@RestController
@RequestMapping(path = "/lot", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "LOT", description = "LOT 조회(상태/이력)")
public class LotQueryController {

    private final LotQueryService queryService;

    @GetMapping("/{lotId}")
    @Operation(summary = "LOT 현재 상태 조회", description = "PES_LOT_MAS 스냅샷. 없으면 404.")
    @ApiResponse(responseCode = "200", description = "현재 상태",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LotView.class),
                    examples = @ExampleObject(value = """
                            {
                              "lotId": "LOT12345", "wfId": "WF56789", "durableId": "DUR001",
                              "statTyp": "REL", "lotSpec": "SPEC-A", "srcSystem": "UI", "userId": "UIUSER01",
                              "createTm": "20260620153000000", "updateTm": "20260620153005000"
                            }""")))
    @ApiResponse(responseCode = "404", description = "LOT 미존재",
            content = @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)))
    public LotView getState(@PathVariable String lotId) {
        return this.queryService.getState(lotId);
    }

    @GetMapping("/{lotId}/history")
    @Operation(summary = "LOT 이력 조회", description = "최신순 페이징. method/from/to(TIMEKEY) 필터 지원.")
    @ApiResponse(responseCode = "200", description = "이력 페이지(최신순)",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "content": [
                                {"timekey": "20260620153005000000", "method": "released", "eventCd": "RELEASED", "statTyp": "REL", "srcSystem": "UI", "userId": "UIUSER01", "correlationId": "TX-20260620-0001"},
                                {"timekey": "20260620153000000000", "method": "created", "eventCd": "CREATED", "statTyp": "NEW", "srcSystem": "UI", "userId": "UIUSER01", "correlationId": "TX-20260620-0001"}
                              ],
                              "page": 0, "size": 20, "totalElements": 2, "totalPages": 1
                            }""")))
    public PesPage<PesHistoryEntry> getHistory(
            @PathVariable String lotId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String method,
            @RequestParam(name = "from", required = false) String fromTimekey,
            @RequestParam(name = "to", required = false) String toTimekey) {
        return this.queryService.getHistory(lotId, method, fromTimekey, toTimekey, page, size);
    }
}
