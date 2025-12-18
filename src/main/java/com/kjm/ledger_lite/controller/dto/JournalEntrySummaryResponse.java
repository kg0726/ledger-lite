package com.kjm.ledger_lite.controller.dto;


import java.time.LocalDate;

/**
 * 전표 목록(요약) API에서 클라이언트에게 내려줄 응답 DTO
 * 엔티티를 그대로 노출하지 않고, 필요한 필드만 전달
 * 이 DTO 리스트를 반환하면 컨트롤러에서 스프링이 자동으로 JSON으로 변환해서 응답함
 */
public record JournalEntrySummaryResponse(
        Long id,
        LocalDate entryDate,
        String description,
        long debitTotal,
        long creditTotal
) {}
