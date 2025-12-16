package com.kjm.ledger_lite.repository;

import com.kjm.ledger_lite.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * AccountRepository
 *
 * ✅ 역할
 * - DB에 Account를 저장/조회/삭제하는 "DB 접근 계층"
 *
 * ✅ 핵심 포인트 (네가 질문한 부분)
 * - 이 파일은 "인터페이스"인데도 동작한다.
 * - 이유: Spring Data JPA가 런타임에 이 인터페이스의 "구현체(프록시 객체)"를 자동 생성한다.
 *
 * ✅ JpaRepository가 제공하는 대표 기능
 * - save(entity): INSERT/UPDATE
 * - findAll(): SELECT *
 * - findById(id): PK로 조회
 * - deleteById(id): 삭제
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * ✅ findByCode는 우리가 "직접 SQL을 쓰지 않아도" 동작한다.
     *
     * 왜?
     * - Spring Data JPA는 메서드 이름 규칙(findBy + 필드명)을 보고
     *   "code 컬럼으로 조회하는 쿼리"를 자동 생성한다.
     *
     * Optional<Account>를 쓰는 이유?
     * - 결과가 있을 수도/없을 수도 있으니 null 대신 Optional로 안전하게 표현
     */
    Optional<Account> findByCode(String code);
}
