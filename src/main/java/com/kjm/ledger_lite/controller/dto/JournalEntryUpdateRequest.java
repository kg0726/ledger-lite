package com.kjm.ledger_lite.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * -PATCH 요청 바디를 받을 DTO
 * 적요(description) 수정에 사용
 */
public record JournalEntryUpdateRequest(
        @NotBlank(message = "description은 공백일 수 없습니다.")
        String description
) {}
