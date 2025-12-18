package com.kjm.ledger_lite.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjm.ledger_lite.domain.Account;
import com.kjm.ledger_lite.repository.AccountRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * JournalEntryControllerTest
 *
 * ✅ 이 테스트의 목적
 * - 실제 서버를 띄우지 않고(MockMvc),
 *   HTTP 요청이 들어왔을 때 스프링이 처리하는 핵심 흐름을 검증한다.
 *
 * ✅ 전체 흐름(요청→처리→응답)
 * - (테스트) MockMvc가 HTTP 요청을 흉내냄
 * - DispatcherServlet이 요청을 받아 Controller 메서드로 라우팅
 * - Controller가 @RequestBody JSON을 DTO로 파싱 + @Valid 검증
 * - Service가 비즈니스 규칙(차/대 합) 검증, 계정 존재 검증, 저장 수행
 * - 예외 발생 시 GlobalExceptionHandler가 400/404를 JSON으로 표준화
 * - 최종적으로 HTTP 응답(status/body)을 MockMvc가 검증
 */
@SpringBootTest
@AutoConfigureMockMvc

/**
 * ✅ 테스트 DB를 "운영/로컬 파일DB"와 분리하기 위한 설정
 * - 네 프로젝트는 현재 H2 file 모드로 쓰고 있어서 테스트가 로컬 DB를 더럽힐 수 있음
 * - 그래서 테스트에서는 mem DB로 강제해서 테스트가 독립적으로 돌게 만든다.
 *
 * ※ application-test.properties를 따로 만들어도 되는데,
 *   지금은 빠르게 끝내려고 TestPropertySource로 박아버림.
 */
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.defer-datasource-initialization=true",
        "spring.sql.init.mode=always"
})

/**
 * ✅ @Transactional
 * - 각 테스트가 끝나면 DB 변경사항이 롤백되어,
 *   테스트끼리 서로 영향을 주지 않도록 한다.
 */
@Transactional
class JournalEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // JSON 문자열 생성/파싱 도구

    @Autowired
    private AccountRepository accountRepository; // data.sql로 생성된 계정과목 id 조회용

    @Test
    @DisplayName("전표 생성 정상: 201 Created + 응답 바디에 생성된 id(숫자)가 온다")
    void create_journalEntry_success_returns201_and_id() throws Exception {
        // -----------------------------
        // 1) 테스트 준비: 계정과목 id 확보
        // - data.sql에 CASH(1000), PRODUCT(1111) 시딩이 되어 있다는 전제
        // - id는 매번 달라질 수 있으니 code로 찾아서 id를 얻는다.
        // -----------------------------
        Long cashId = findAccountIdByCode("1000");
        Long productId = findAccountIdByCode("1111");

        // -----------------------------
        // 2) 요청 JSON 만들기
        // - POST /api/journal-entries
        // - 차변(PRODUCT) 10000, 대변(CASH) 10000 (합계 일치)
        // -----------------------------
        String jsonBody = objectMapper.writeValueAsString(Map.of(
                "entryDate", "2025-12-17",
                "description", "Buy product with cash",
                "lines", List.of(
                        Map.of("dcType", "DEBIT", "amount", 10000, "accountId", productId),
                        Map.of("dcType", "CREDIT", "amount", 10000, "accountId", cashId)
                )
        ));

        // -----------------------------
        // 3) 요청→처리→응답 검증
        // - 201 Created
        // - 응답 바디는 "1" 같은 숫자 문자열(네가 이미 curl로 확인했던 형태)
        // -----------------------------
        mockMvc.perform(
                        post("/api/journal-entries")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonBody)
                )
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print()) // ✅ 추가
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.id").value(Matchers.greaterThan(0)));

    }

    @Test
    @DisplayName("전표 생성 실패(차/대 불일치): 400 Bad Request + message='Debit sum must equal credit sum'")
    void create_journalEntry_debitCreditMismatch_returns400() throws Exception {
        Long cashId = findAccountIdByCode("1000");
        Long productId = findAccountIdByCode("1111");

        // 차변 10000, 대변 9000 (불일치)
        String jsonBody = objectMapper.writeValueAsString(Map.of(
                "entryDate", "2025-12-17",
                "description", "Mismatch test",
                "lines", List.of(
                        Map.of("dcType", "DEBIT", "amount", 10000, "accountId", productId),
                        Map.of("dcType", "CREDIT", "amount", 9000, "accountId", cashId)
                )
        ));

        mockMvc.perform(
                        post("/api/journal-entries")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonBody)
                )
                // ✅ 네 서비스에서 IllegalArgumentException("Debit sum must equal credit sum") 던짐
                // ✅ 전역 핸들러가 400 JSON으로 표준화
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debit sum must equal credit sum"))
                .andExpect(jsonPath("$.path").value("/api/journal-entries"));
    }

    @Test
    @DisplayName("전표 생성 실패(없는 계정): 404 Not Found + message='Account not found: 999'")
    void create_journalEntry_accountNotFound_returns404() throws Exception {
        Long cashId = findAccountIdByCode("1000");

        String jsonBody = objectMapper.writeValueAsString(Map.of(
                "entryDate", "2025-12-17",
                "description", "Account not found test",
                "lines", List.of(
                        // 존재하지 않는 accountId=999 사용
                        Map.of("dcType", "DEBIT", "amount", 10000, "accountId", 999),
                        Map.of("dcType", "CREDIT", "amount", 10000, "accountId", cashId)
                )
        ));

        mockMvc.perform(
                        post("/api/journal-entries")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonBody)
                )
                // ✅ 서비스에서 ResourceNotFoundException("Account not found: 999") 던짐
                // ✅ 전역 핸들러가 404 JSON으로 표준화
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found: 999"))
                .andExpect(jsonPath("$.path").value("/api/journal-entries"));
    }

    /**
     * 테스트 유틸: code로 Account id를 찾아오는 메서드
     *
     * ✅ 왜 필요한가?
     * - 테스트에서 accountId를 하드코딩하면(DB가 바뀌면) 깨지기 쉬움
     * - code는 우리가 시딩으로 고정했으니 안정적인 키로 사용 가능
     */
    private Long findAccountIdByCode(String code) {
        Account account = accountRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Test seed account not found. code=" + code));
        return account.getId();
    }
}
