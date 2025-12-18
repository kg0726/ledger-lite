package com.kjm.ledger_lite.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JournalEntry (전표 헤더) 엔티티
 *
 * ✅ 역할
 * - 전표 1건의 "공통 정보"를 담는다. (날짜, 설명, 생성일시 등)
 * - 전표는 여러 개의 분개 라인(JournalLine)을 가진다. (1:N 관계)
 *
 * ✅ DB 관점
 * - JOURNAL_ENTRY 테이블로 매핑됨
 * - PK: id
 * - 컬럼: entryDate, description, createdAt ...
 *
 * ✅ 왜 헤더/라인으로 쪼개나?
 * - 회계 전표는 "전표 기본정보(헤더)" + "분개 라인(상세)" 구조가 표준이다.
 * - 실무에서도 이 구조로 조회/수정/승인/집계가 돌아간다.
 */
@Entity
public class JournalEntry {

    /** 전표 PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 전표 날짜(문자열 버전)
     * - 예: "2025-12-16"
     * - 지금은 입문 단계라 LocalDate 대신 String으로 먼저 간다(파싱 난이도↓)
     */
    @Column(nullable = false)
    private String entryDate;

    /** 전표 설명(적요) */
    @Column(nullable = false)
    private String description;

    /** 전표 생성 시각 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * ✅ 전표 1개가 분개 라인 여러 개를 가진다. (1:N)
     *
     * mappedBy = "journalEntry"
     * - "연관관계의 주인"은 JournalLine.journalEntry 필드다.
     * - 즉, 실제 FK(외래키)는 JOURNAL_LINE 테이블에 들어간다.
     *
     * cascade = CascadeType.ALL
     * - JournalEntry를 save하면 연결된 JournalLine들도 함께 save 된다.
     * - "전표 저장" 한 번에 "분개 라인 저장"까지 같이 처리 가능
     *
     * orphanRemoval = true
     * - 전표에서 라인을 제거하면 DB에서도 제거(고아 데이터 방지)
     */
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalLine> lines = new ArrayList<>();

    /** JPA 기본 생성자(필수) */
    protected JournalEntry() {}

    /** 신규 전표 생성 시 사용하는 생성자 */
    public JournalEntry(String entryDate, String description) {
        this.entryDate = entryDate;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * ✅ 전표에 분개 라인을 추가하는 편의 메서드
     *
     * 중요한 이유:
     * - 양방향 연관관계에서 "두 객체의 연결"을 한 곳에서 안전하게 처리하기 위함.
     *
     * 여기서 하는 일:
     * 1) lines 리스트에 line 추가
     * 2) line 쪽에도 "내가 속한 전표"를 설정 (FK가 채워지게 됨)
     */
    public void addLine(JournalLine line) {
        this.lines.add(line);
        line.setJournalEntry(this); // JournalLine에 전표를 연결(외래키 역할)
    }

    // 적요 변경 메서드
    public void changeDescription(String description) {
        this.description = description;
    }

    // ===== Getter =====
    public Long getId() { return id; }
    public String getEntryDate() { return entryDate; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<JournalLine> getLines() { return lines; }
}
