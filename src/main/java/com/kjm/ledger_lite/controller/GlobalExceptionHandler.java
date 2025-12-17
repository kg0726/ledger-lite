package com.kjm.ledger_lite.controller;

import com.kjm.ledger_lite.controller.dto.ApiErrorResponse;
import com.kjm.ledger_lite.exceiption.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * - 프로젝트 전체 컨트롤러에서 발생하는 예외를 한곳에서 처리한다.
 * @RestControllerAdvier
 * - 전역 컨트롤러 조언자: 컨트롤러에서 예외가 터지면 여기서 가로채서 응답을 만든다.
 */

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 1. 일부러 던지는 비지니스 예외(계정과목명 중복, 차대 검증 불가 등)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    // 2. @Valid 검증 실패를 400으로 통일(NotBlank 위반, NotNull 위반 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        
        // 검증 에러 메시지는 종류가 많아서 우선 첫번째 메시지만 대표로 내려줌
        // todo 나중에 필드별 에러 목록을 내려주는 형태로 확장 가능
        String message = ex.getBindingResult().getAllErrors().isEmpty()
                ? "Validation error"
                : ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    // 3. JSON 파싱 자체가 실패한 경우 400으로 통일(문법 깨짐, UTF-8 문제, 타입 불일치 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handelJsonParse (
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                "Invalid request body (JSON parse failed)",
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    // 4. 존재하지 않는 리소스(404) 처리
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handelNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;

        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
