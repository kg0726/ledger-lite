package com.kjm.ledger_lite.controller;

import com.kjm.ledger_lite.controller.dto.JournalEntryCreateRequest;
import com.kjm.ledger_lite.domain.Account;
import com.kjm.ledger_lite.domain.JournalEntry;
import com.kjm.ledger_lite.domain.JournalLine;
import com.kjm.ledger_lite.repository.AccountRepository;
import com.kjm.ledger_lite.repository.JournalEntryRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * JournalEntryController
 *
 * ✅ 역할
 * - 전표 등록 요청을 받는다.
 * - 요청 JSON을 DTO로 변환(@RequestBody)
 * - DTO 유효성 검증(@Valid)
 * - 핵심 비즈니스 룰(차변합=대변합)을 검증한다.
 * - accountId로 Account를 조회하여 분개 라인을 구성한다.
 * - 전표(JournalEntry) 저장(save) 시 cascade로 분개 라인까지 같이 저장한다.
 */
@RestController
@RequestMapping("/api/journal-entries")
public class JournalEntryController {

    /**
     * ✅ final + 생성자 주입 상기
     * - 스프링이 실행 시점에 Repository 구현체(프록시)를 만들어서 주입한다.
     */
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;

    public JournalEntryController(JournalEntryRepository journalEntryRepository,
                                  AccountRepository accountRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * POST /api/journal-entries
     *
     * ✅ 요청 처리 흐름(요청→처리→응답)
     * 1) DispatcherServlet이 URL+Method로 이 메서드로 라우팅
     * 2) @RequestBody: JSON → JournalEntryCreateRequest 변환(Jackson)
     * 3) @Valid: DTO 검증(@NotBlank/@NotEmpty/@NotNull) 실패 시 400
     * 4) 차변합/대변합 계산 후 불일치면 IllegalArgumentException 발생(우리는 전역핸들러로 400)
     * 5) 전표 엔티티 생성
     * 6) 라인 반복:
     *    - accountId로 Account 조회
     *    - JournalLine 엔티티 생성
     *    - entry.addLine로 양방향 연결(FK 세팅)
     * 7) journalEntryRepository.save(entry)
     *    - Repository 구현체(프록시)가 DB 저장 처리
     *    - cascade=ALL 덕분에 JournalLine들도 함께 INSERT
     * 8) 성공 시 201 Created 반환
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody JournalEntryCreateRequest req) {

        // ===== 1) 차변/대변 합계 계산 =====
        long debitSum = 0;
        long creditSum = 0;

        for (JournalEntryCreateRequest.Line line : req.lines()) {
            // dcType은 "DEBIT" 또는 "CREDIT"만 허용(오타 방지)
            if ("DEBIT".equals(line.dcType())) {
                debitSum += line.amount();
            } else if ("CREDIT".equals(line.dcType())) {
                creditSum += line.amount();
            } else {
                // 잘못된 값은 비즈니스 예외로 처리 → 전역 핸들러가 400으로 응답하도록
                throw new IllegalArgumentException("dcType must be DEBIT or CREDIT");
            }
        }

        // ===== 2) 핵심 비즈니스 룰: 차변합 = 대변합 =====
        if (debitSum != creditSum) {
            throw new IllegalArgumentException("Debit sum must equal credit sum");
        }

        // ===== 3) 전표 헤더 엔티티 생성 =====
        JournalEntry entry = new JournalEntry(req.entryDate(), req.description());

        // ===== 4) 분개 라인 엔티티 구성 =====
        for (JournalEntryCreateRequest.Line line : req.lines()) {

            // accountId로 계정과목 조회(없으면 잘못된 요청)
            Account account = accountRepository.findById(line.accountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + line.accountId()));

            // 분개 라인 생성(전표 연결은 addLine에서 수행)
            JournalLine journalLine = new JournalLine(line.dcType(), line.amount(), account);

            // 전표-분개 연결(양방향, FK 세팅)
            entry.addLine(journalLine);
        }

        // ===== 5) 저장 =====
        // save(entry) 호출 →
        // (스프링이 만든 JournalEntryRepository 구현체/프록시) →
        // JPA(EntityManager) →
        // Hibernate →
        // JDBC →
        // H2 DB에 INSERT
        //
        // 그리고 cascade=ALL 덕분에 JournalLine도 함께 INSERT 된다.
        journalEntryRepository.save(entry);
    }
}
