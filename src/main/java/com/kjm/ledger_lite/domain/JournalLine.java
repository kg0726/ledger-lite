package com.kjm.ledger_lite.domain;

import jakarta.persistence.*;

/**
 * JournalLine (분개 라인) 엔티티
 *
 * ✅ 역할
 * - 한 전표(JournalEntry)에 속한 "상세 분개 1줄"을 나타낸다.
 * - 차/대변 구분(dcType), 금액(amount), 계정과목(account)을 가진다.
 *
 * ✅ DB 관점
 * - JOURNAL_LINE 테이블로 매핑됨
 * - FK 2개:
 *   1) account_id  -> Account(계정과목) 참조
 *   2) journal_entry_id -> JournalEntry(전표) 참조
 */
@Entity
public class JournalLine {

    /** 분개 라인 PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 차/대변 구분
     * - 지금은 단순화를 위해 String("DEBIT", "CREDIT")로 처리
     * - 나중에 enum으로 바꾸면 실수(오타)를 더 줄일 수 있음
     */
    @Column(nullable = false)
    private String dcType;

    /**
     * 금액
     * - 회계는 소수점/원단위/환율 등 이슈가 있어서 실무에선 BigDecimal 많이 씀
     * - 우리는 미니 프로젝트라 Long(정수)로 시작
     */
    @Column(nullable = false)
    private Long amount;

    /**
     * ✅ 분개 라인은 "어떤 계정과목인지"를 반드시 알아야 한다. (N:1)
     * optional=false: 반드시 연결되어야 함(NULL 금지)
     */
    @ManyToOne(optional = false)
    private Account account;

    /**
     * ✅ 분개 라인은 "어떤 전표에 속했는지" 반드시 알아야 한다. (N:1)
     * 이 필드가 FK를 가진 "연관관계의 주인"이다.
     */
    @ManyToOne(optional = false)
    private JournalEntry journalEntry;

    /** JPA 기본 생성자(필수) */
    protected JournalLine() {}

    /** 신규 분개 라인 생성(전표 연결은 JournalEntry.addLine에서 수행) */
    public JournalLine(String dcType, Long amount, Account account) {
        this.dcType = dcType;
        this.amount = amount;
        this.account = account;
    }

    /**
     * 전표 연결 setter
     * - 패키지 private(기본 접근)로 둬서 "같은 도메인 패키지"에서만 호출되게 제한
     * - 우리는 JournalEntry.addLine에서만 연결하도록 유도하고 싶음(통제)
     */
    void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    // ===== Getter =====
    public Long getId() { return id; }
    public String getDcType() { return dcType; }
    public Long getAmount() { return amount; }
    public Account getAccount() { return account; }
    public JournalEntry getJournalEntry() { return journalEntry; }
}
