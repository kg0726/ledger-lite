package com.kjm.ledger_lite.repository;

import com.kjm.ledger_lite.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
