package com.kjm.ledger_lite.controller.dto;

import java.util.List;

/**
 * 전표를 조회할 때 필요한 정보만 깨끗한 JSON으로 반환함
 * 엔티티를 그대로 반환하지 않고 DTO로 출력 항목을 통제
 */
public record JournalEntryDetailResponse(
        Long id,
        String entryDate,
        String description,
        List<Line> lines
) {
    /**
     * Line 응답 DTO
     * 프론트/테스트에서 무슨 계정과목인지 확인이 바로 가능해야 함
     */
    public record Line(
            String dcType,
            Long amount,
            Long accountId,
            String accountCode,
            String accountName
    ) {}
}
