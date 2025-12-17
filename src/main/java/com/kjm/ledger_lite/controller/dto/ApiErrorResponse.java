package com.kjm.ledger_lite.controller.dto;

import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * 에러 응답 DTO
 * - 예외가 발생햇을 때 클라이언트에게 내려줄 JSO?N 응답 형식을 통일
 */
public record ApiErrorResponse (
    LocalDateTime timestamp, // 언제 에러가 났는지
    int status, // HTTP 상태 코드
    String error, // 상태 이름
    String message, // 에러 메시지
    String path // 어떤 URL 에서 발생했는지
) {}
