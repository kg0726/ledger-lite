package com.kjm.ledger_lite.repository;

import com.kjm.ledger_lite.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * JournalEntryRepository
 *
 * ✅ 역할
 * - JournalEntry를 DB에 저장/조회하는 Repository 인터페이스
 *
 * ✅ 상기(중요)
 * - 이 인터페이스의 "구현체"는 우리가 만들지 않는다.
 * - Spring Data JPA가 서버 실행 시점에 자동으로 프록시 구현체를 생성한다.
 */
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    /**
     * 전표 목록 조회 (라인 + 계정까지 한번에 로딩)
     *
     * fetch join
     * - JournalEntry(부모) -> lines(자식 컬렉션) -> account(계정과목)까지
     * 목록 조회 시 합계 계산에 필요한 요소들읊 같이 가져온다
     *
     * distinct로 row 중복을 방지
     */
    @Query("""
            select distinct je
            from JournalEntry je
            left join fetch je.lines l
            left join fetch l.account a
            order by je.entryDate desc, je.id desc
            """)
    List<JournalEntry> findAllWithLinesAndAccountOrderByEntryDateDesc();
}
