package com.kjm.ledger_lite.controller.dto;

/**
 * 전표 생성 성공 시 생성된 전표 ID를 JSON객체 형태로 반환
 */
public record JournalEntryCreateResponse(
        Long id
) {}
