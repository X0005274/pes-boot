package com.playtogether.pes.lot.web;

import com.playtogether.pes.common.error.PesNotFoundException;
import com.playtogether.pes.common.workflow.PesProcessResult;
import com.playtogether.pes.common.workflow.PesProcessRollbackException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 진입점 공통 예외 처리. 계약 위반/검증 실패/미지원 도메인을 표준 ProblemDetail 로 변환.
 */
@RestControllerAdvice
public class PesApiExceptionHandler {

    /** @Valid 검증 실패 (entityType 계약, workflow 비어있음, meta null 등). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onValidation(MethodArgumentNotValidException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("Message validation failed");
        detail.setDetail(ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("invalid message"));
        return detail;
    }

    /** 처리 실패로 롤백된 경우. 구조화된 결과(status=FAILED)를 200 으로 반환한다. */
    @ExceptionHandler(PesProcessRollbackException.class)
    public ResponseEntity<PesProcessResult> onRollback(PesProcessRollbackException ex) {
        return ResponseEntity.ok(ex.getResult());
    }

    /** 조회 대상 없음 → 404. */
    @ExceptionHandler(PesNotFoundException.class)
    public ProblemDetail onNotFound(PesNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        detail.setTitle("Resource not found");
        detail.setDetail(ex.getMessage());
        return detail;
    }

    /** assertContract() 등 계약 위반. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail onContract(IllegalArgumentException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("Contract violation");
        detail.setDetail(ex.getMessage());
        return detail;
    }

    /** 라우팅 대상 도메인 핸들러 미등록(WF/DURABLE 미구현 등). */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ProblemDetail onUnsupported(UnsupportedOperationException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.NOT_IMPLEMENTED);
        detail.setTitle("Domain handler not available");
        detail.setDetail(ex.getMessage());
        return detail;
    }
}
