package com.kjm.ledger_lite.controller;

import com.kjm.ledger_lite.controller.dto.*;
import com.kjm.ledger_lite.service.JournalEntryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * JournalEntryController
 * HTTP 요청을 받고 JSON -> DTO 변환 후 검증
 * Service에 비즈니스 로직 및 DB 조작을 위임
 * HTTP 응답만을 만듦
 */
@RestController
@RequestMapping("/api/journal-entries")
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    public JournalEntryController(JournalEntryService journalEntryService) {
        this.journalEntryService = journalEntryService;
    }

    /**
     * POST /api/journal-entries
     * Tomcat 수신 -> DispatcherServlet 라우팅
     * @RequestBody로 JSON을 JournalEntryCreateRequest로 변환
     * @Valid로 검증
     * Service.create(req) 호출
     *  - 차대검증
     *  - 계정과목 존재 검증
     *  - 전표라인 생성
     *  - 트랜잭션 저장
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JournalEntryCreateResponse create(@Valid @RequestBody JournalEntryCreateRequest req) {
        // Service가 생성한 전표 id 그대로를 반환
        Long id =  journalEntryService.create(req);
        // DTO를 통해 JSON 객체 형태로 변환하여 응답
        return new JournalEntryCreateResponse(id);
    }

    /**
     * GET /api/journal-entries/{id}
     * 특정 id의 전표를 상세조회
     */
    @GetMapping("/{id}")
    public JournalEntryDetailResponse get(@PathVariable Long id) {
        return journalEntryService.get(id);
    }

    /**
     * 전표 목록 조회
     * GET /api/journal-entries
     */
    @GetMapping
    public List<JournalEntrySummaryResponse> list() {
        return journalEntryService.listSummaries();
    }

    /**
     * id를 routing
     */
    @PatchMapping("/{id}")
    public JournalEntryDetailResponse updateDescription(
            @PathVariable Long id,
            @Valid @RequestBody JournalEntryUpdateRequest req
            ) {
        return journalEntryService.updateDescription(id, req);
    }
}
