package com.kjm.ledger_lite.service;

import com.kjm.ledger_lite.controller.dto.JournalEntryCreateRequest;
import com.kjm.ledger_lite.controller.dto.JournalEntryDetailResponse;
import com.kjm.ledger_lite.controller.dto.JournalEntrySummaryResponse;
import com.kjm.ledger_lite.controller.dto.JournalEntryUpdateRequest;
import com.kjm.ledger_lite.domain.Account;
import com.kjm.ledger_lite.domain.JournalEntry;
import com.kjm.ledger_lite.domain.JournalLine;
import com.kjm.ledger_lite.exceiption.ResourceNotFoundException;
import com.kjm.ledger_lite.repository.AccountRepository;
import com.kjm.ledger_lite.repository.JournalEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + line.accountId()));

            JournalLine journalLine = new JournalLine(line.dcType(), line.amount(), account);
            // 전표에 분개 라인 추가
            entry.addLine(journalLine);
        }
        // 5. 저장. save 호출
        JournalEntry saved = journalEntryRepository.save(entry);
        // 생성된 전표 id 반환

        return saved.getId();
    }

    /**
     * 전표 단건 조회
     * 1. findById로 전표 조회
     * 없으면 전역 핸들러가 404 반환
     * 있으면 DTO로 변환하여 응답
     */
    @Transactional(readOnly = true)
    public JournalEntryDetailResponse get(Long id) {
        // 조회 할 전표 할당
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JournalEntry not found: " + id));
        // 엔티티 -> DTO 변환
        List<JournalEntryDetailResponse.Line> lines = entry.getLines().stream()
                .map(this::toLineDto)
                .toList();

        return new JournalEntryDetailResponse(
                entry.getId(),
                entry.getEntryDate(),
                entry.getDescription(),
                lines
        );
    }
    /**
     * JournalLine 엔티티를 Line DTO로 변환하는 메서드
     */
    private JournalEntryDetailResponse.Line toLineDto(JournalLine line) {
        return new JournalEntryDetailResponse.Line(
                line.getDcType(),
                line.getAmount(),
                line.getAccount().getId(),
                line.getAccount().getCode(),
                line.getAccount().getName()
        );
    }

    /**
     * 전표 목록 (요약)조회
     * 
     * Controller에서 이 메서드 호출
     * Repository가 전표 + 라인 + 계정까지 fetch join을 통해 가져옴
     * Service가 각 전표의 라인을 순회하며 차/대 합계를 계산
     * Summary DTO 리스트를 만들어 반환
     */
    @Transactional(readOnly = true)
    public List<JournalEntrySummaryResponse> listSummaries() {
        // 1. 전표 목록을 DB에서 조회
        List<JournalEntry> entries =
                journalEntryRepository.findAllWithLinesAndAccountOrderByEntryDateDesc();
        // 2. 엔티티 -> 요약 DTO로 변환
        List<JournalEntrySummaryResponse> result = new ArrayList<>();

        for (JournalEntry je : entries) {
            long debitTotal = 0L;
            long creditTotal = 0L;

            // 라인들을 순회하며 차/대 합계 계산
            for (JournalLine line : je.getLines()) {
                if ("DEBIT".equals(line.getDcType())) {
                    debitTotal += line.getAmount();
                } else if ("CREDIT".equals(line.getDcType())) {
                    creditTotal += line.getAmount();
                }
            }
            // LocalData 형식으로 변환
            LocalDate entryDate = LocalDate.parse(je.getEntryDate());
            result.add(new JournalEntrySummaryResponse(
                    je.getId(),
                    entryDate,
                    je.getDescription(),
                    debitTotal,
                    creditTotal
            ));
        }
        return result;
    }

    /**
     * 전표 적요 수정
     * Controller가 id, req를 받아 Service 호출
     * Service가 전표 조회
     * 엔티티의 description 변경
     * 트랜잭션 커밋 시점에 JPA Dirty Checking으로 UPDATE 반영
     * 수정된 전표를 Detail DTO로 만들어 반환
     */
    @Transactional
    public JournalEntryDetailResponse updateDescription(Long id, JournalEntryUpdateRequest req) {
        // 수정할 대상 전표 조회
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JournalEntry not found"));
        // 엔티티 상태 변경(Dirty Check 대상)
        entry.changeDescription(req.description());
        // 수정된 엔티티 -> DTO 변환
        List<JournalEntryDetailResponse.Line> lines = entry.getLines().stream()
                .map(this::toLineDto)
                .toList();
        return new JournalEntryDetailResponse(
                entry.getId(),
                entry.getEntryDate(),
                entry.getDescription(),
                lines
        );
    }
}
