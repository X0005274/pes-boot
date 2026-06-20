package com.playtogether.pes.hub;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HubDB 적재 수동 트리거. (스케줄러 도입 시 동일 서비스 재사용)
 */
@RestController
@RequestMapping(path = "/hub", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Hub", description = "HubDB → PES 적재")
public class HubIngestionController {

    private final HubIngestionService ingestionService;

    @PostMapping("/ingest")
    @Operation(summary = "HubDB 적재 트리거", description = "HubSource 배치를 동일 Biz 파이프라인으로 적재. mode(mas/his)에 따라 동작.")
    public HubIngestResult ingest(@RequestParam(name = "max", defaultValue = "100") int max) {
        return this.ingestionService.ingest(max);
    }
}
