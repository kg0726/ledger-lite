package com.kjm.ledger_lite.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * JournalEntryCreateRequest (요청 DTO)
 *
 * ✅ 역할
 * - 클라이언트가 전표 등록 시 보내는 JSON 구조를 "명확히" 정의한다.
 *
 * ✅ JSON 예시
 * {
 *   "entryDate": "2025-12-16",
 *   "description": "sample entry",
 *   "lines": [
 *     {"dcType":"DEBIT","amount":10000,"accountId":1},
 *     {"dcType":"CREDIT","amount":10000,"accountId":2}
 *   ]
 * }
 *
 * ✅ 검증
 * - entryDate/description: 비어 있으면 안 됨
 * - lines: 최소 1줄 이상 있어야 함
 * - line 내부: dcType/amount/accountId 필수
 */
public record JournalEntryCreateRequest(
        @NotBlank String entryDate,
        @NotBlank String description,
        @NotEmpty List<Line> lines
) {
    /**
     * Line (분개 라인 DTO)
     * - accountId는 "어떤 계정과목을 참조할지"를 의미
     * - 서버에서 accountId로 Account를 조회한 후 JournalLine 엔티티를 만든다.
     */
    public record Line(
            @NotBlank String dcType,
            @NotNull Long amount,
            @NotNull Long accountId
    ) {}
}
