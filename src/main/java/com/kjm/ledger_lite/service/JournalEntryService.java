package com.kjm.ledger_lite.service;

import com.kjm.ledger_lite.controller.dto.JournalEntryCreateRequest;
import com.kjm.ledger_lite.domain.Account;
import com.kjm.ledger_lite.domain.JournalEntry;
import com.kjm.ledger_lite.domain.JournalLine;
import com.kjm.ledger_lite.repository.AccountRepository;
import com.kjm.ledger_lite.repository.JournalEntryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * 회계 규칙(차변합 = 대변합) 같은 비즈니스 로직을 Controller 밖으로 분리
 * Controller는 요청/응답에 집중, Service는 업무 규칙 + 데이터 생성/저장에 집중
 */
@Service
public class JournalEntryService {
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    
    public JournalEntryService(JournalEntryRepository journalEntryRepository,
                               AccountRepository accountRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountRepository = accountRepository;
    }
    
    // 전표 저장 메서드
    // 이 메서드 안의 DB 작업을 하나의 트렌잭션으로 묶음
    // 중간에 DB 관련 에러가 발생해도 저장이 모두 롤백되어 데이터가 꼬이지 않음
    @Transactional
    public Long create(JournalEntryCreateRequest req) {
        // 1. 차번/대변 합계 계산 밑 dcType 유효성 검증
        long debitSum = 0;
        long creditSum = 0;

        for (JournalEntryCreateRequest.Line line : req.lines()) {
            if ("DEBIT".equals(line.dcType())) {
                debitSum += line.amount();
            } else if ("CREDIT".equals(line.dcType())) {
                creditSum += line.amount();
            } else {
                // 차변 대변을 잘못 기입한 경우
                throw new IllegalArgumentException("dcType must be DEBIT or CREDIT");
            }
        }

        // 2. 차변합과 대변합이 일치하는지 검증
        if (debitSum != creditSum) {
            throw new IllegalArgumentException("Debit sum must equal credit sum");
        }

        // 3. 전표 엔티티 생성
        JournalEntry entry = new JournalEntry(req.entryDate(), req.description());

        // 4. 라인 생성 + 계정과목 존재 검증 + 전표에 연결
        for (JournalEntryCreateRequest.Line line: req.lines()) {
            // accountId로 계정과목 조회
            Account account = accountRepository.findById(line.accountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + line.accountId()));

            JournalLine journalLine = new JournalLine(line.dcType(), line.amount(), account);
            // 전표에 분개 라인 추가
            entry.addLine(journalLine);
        }
        // 5. 저장. save 호출
        JournalEntry saved = journalEntryRepository.save(entry);
        // 생성된 전표 id 반환

        return saved.getId();
    }
}
