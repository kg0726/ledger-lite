package com.kjm.ledger_lite.domain;

import jakarta.persistence.*;

/**
 * Account (계정과목) 엔티티
 *
 * ✅ 역할
 * - "DB 테이블(ACCOUNT)"로 매핑되는 자바 클래스
 * - 회계 데이터에서 계정과목은 모든 분개 라인이 참조하는 '기준 데이터(master)'가 된다.
 *
 * ✅ 왜 엔티티가 필요?
 * - 우리가 DB에 저장/조회할 데이터 구조를 "자바 코드"로 정의해두면
 *   JPA(Hibernate)가 이 정의를 보고 테이블 생성/쿼리 실행을 도와준다.
 */
@Entity // 이 클래스는 JPA가 관리하는 "테이블 매핑 대상"임을 선언
public class Account {

    /**
     * ✅ PK(기본키)
     * - 각 계정과목을 유일하게 식별하는 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // IDENTITY: DB가 id 값을 자동 증가(autoincrement)시키는 방식
    private Long id;

    /**
     * ✅ 계정과목 코드 (중복 금지)
     * - 예: 1000(현금), 4000(매출) 같은 코드
     *
     * nullable=false: NULL 저장 금지(데이터 품질/통제)
     * unique=true: 코드 중복 금지(중복되면 조회/집계가 망가짐)
     */
    @Column(nullable = false, unique = true)
    private String code;

    /**
     * ✅ 계정과목명
     * - 예: 현금, 매출, 지급수수료...
     * nullable=false: 필수값
     */
    @Column(nullable = false)
    private String name;

    /**
     * ✅ JPA가 반드시 필요로 하는 "기본 생성자"
     *
     * 왜 필요?
     * - JPA는 DB에서 데이터를 읽어올 때 리플렉션으로 객체를 생성하는데,
     *   이때 "파라미터 없는 생성자"가 없으면 생성 자체가 불가능해짐.
     *
     * protected인 이유?
     * - JPA는 접근할 수 있게 열어두되,
     * - 외부 코드에서 실수로 new Account()를 막 호출하는 건 제한하기 위함.
     */
    protected Account() {}

    /**
     * ✅ 우리가 직접 객체를 만들 때 사용하는 생성자
     * - 신규 계정과목 등록 시 사용
     */
    public Account(String code, String name) {
        this.code = code;
        this.name = name;
    }

    // ===== Getter: 외부에서 값을 읽을 때 사용 =====
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}
