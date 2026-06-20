package com.playtogether.pes.common.query;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 페이지 조회 응답(스프링 Page 노출 대신 안정적 스키마 사용).
 */
@Schema(description = "페이지 응답")
public record PesPage<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PesPage<T> of(List<T> content, int page, int size,
                                    long totalElements, int totalPages) {
        return new PesPage<>(content, page, size, totalElements, totalPages);
    }
}
