package com.kjm.ledger_lite.controller;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * JournalEntryControllerTest
 *
 * ✅ 목적
 * - 전표 생성/조회/목록/적요수정 기능이 정상 동작하는지, 그리고 예외가 표준 응답으로 떨어지는지 검증
 *
 * ✅ 전체 흐름(요청→처리→응답)
 * - MockMvc(HTTP 시뮬레이션)
 * - DispatcherServlet → Controller → Service(회계 규칙/검증) → Repository(JPA) → H2 DB
 * - 정상: 200/201
 * - 실패: 전역 예외 핸들러가 400/404를 JSON으로 표준화
 */
@SpringBootTest
@AutoConfigureMockMvc

@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.defer-datasource-initialization=true",
        "spring.sql.init.mode=always"
})

@Transactional
class JournalEntryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AccountRepository accountRepository;

    // -------------------------
    // ✅ Helper: seed 계정(code)으로 id 얻기
    // - data.sql에 CASH(1000), PRODUCT(1111) 시딩되어 있다는 전제
    // - id를 하드코딩하지 않고 code로 찾아 “깨지지 않는 테스트”를 만든다.
    // -------------------------
    private Long findAccountIdByCode(String code) {
        Account account = accountRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Test seed account not found. code=" + code));
        return account.getId();
    }

    // -------------------------
    // ✅ Helper: 전표 생성 요청 바디 만들기(차/대 합 일치)
    // -------------------------
    private String buildValidCreateBody(String entryDate, String description, long amount) throws Exception {
        Long cashId = findAccountIdByCode("1000");
        Long productId = findAccountIdByCode("1111");

        return objectMapper.writeValueAsString(Map.of(
                "entryDate", entryDate,
                "description", description,
                "lines", List.of(
                        Map.of("dcType", "DEBIT", "amount", amount, "accountId", productId),
                        Map.of("dcType", "CREDIT", "amount", amount, "accountId", cashId)
                )
        ));
    }

    // -------------------------
    // ✅ Helper: 전표 1건 생성하고 id 반환
    // - POST /api/journal-entries → 201 + {"id":숫자}
    // -------------------------
    private long createOneAndReturnId(String entryDate, String description, long amount) throws Exception {
        String body = buildValidCreateBody(entryDate, description, amount);

        String response = mockMvc.perform(post("/api/journal-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        return root.get("id").asLong();
    }

    @Test
    @DisplayName("전표 생성 정상: 201 Created + 응답 JSON에 생성 id가 포함된다")
    void create_journalEntry_success_returns201_and_id() throws Exception {
        String body = buildValidCreateBody("2025-12-17", "Buy product with cash", 10000);

        mockMvc.perform(post("/api/journal-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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
        String body = objectMapper.writeValueAsString(Map.of(
                "entryDate", "2025-12-17",
                "description", "Mismatch test",
                "lines", List.of(
                        Map.of("dcType", "DEBIT", "amount", 10000, "accountId", productId),
                        Map.of("dcType", "CREDIT", "amount", 9000, "accountId", cashId)
                )
        ));

        mockMvc.perform(post("/api/journal-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debit sum must equal credit sum"))
                .andExpect(jsonPath("$.path").value("/api/journal-entries"));
    }

    @Test
    @DisplayName("전표 생성 실패(없는 계정): 404 Not Found + message='Account not found: 999'")
    void create_journalEntry_accountNotFound_returns404() throws Exception {
        Long cashId = findAccountIdByCode("1000");

        String body = objectMapper.writeValueAsString(Map.of(
                "entryDate", "2025-12-17",
                "description", "Account not found test",
                "lines", List.of(
                        Map.of("dcType", "DEBIT", "amount", 10000, "accountId", 999),
                        Map.of("dcType", "CREDIT", "amount", 10000, "accountId", cashId)
                )
        ));

        mockMvc.perform(post("/api/journal-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found: 999"))
                .andExpect(jsonPath("$.path").value("/api/journal-entries"));
    }

    @Test
    @DisplayName("전표 단건 조회 정상: 생성된 전표를 GET으로 조회하면 200 + lines 포함 응답이 온다")
    void get_journalEntry_detail_success_returns200_and_lines() throws Exception {
        // ✅ 1) 생성 → id 확보
        long id = createOneAndReturnId("2025-12-17", "Buy product with cash", 10000);

        // ✅ 2) 단건 조회 → DTO 구조/필드 검증
        mockMvc.perform(get("/api/journal-entries/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.entryDate").value("2025-12-17"))
                .andExpect(jsonPath("$.description").value("Buy product with cash"))
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.lines[0].dcType").exists())
                .andExpect(jsonPath("$.lines[0].amount").exists())
                .andExpect(jsonPath("$.lines[0].accountId").exists())
                .andExpect(jsonPath("$.lines[0].accountCode").exists())
                .andExpect(jsonPath("$.lines[0].accountName").exists());
    }

    @Test
    @DisplayName("전표 목록(요약) 조회 정상: 200 + 배열 + 요약 필드(debitTotal/creditTotal) 포함")
    void list_journalEntries_summary_returns200_and_summaryFields() throws Exception {
        // ✅ 2건 생성(날짜를 다르게 줘서 정렬/목록 확인이 쉬움)
        createOneAndReturnId("2025-12-17", "First entry", 10000);
        createOneAndReturnId("2025-12-18", "Second entry", 20000);

        mockMvc.perform(get("/api/journal-entries"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                // 최소 2건 이상 존재(우리가 방금 2건 생성했으니까)
                .andExpect(jsonPath("$.length()").value(Matchers.greaterThanOrEqualTo(2)))
                // 요약 DTO 필드 존재 여부(프로젝트 구현 형태에 맞춘 “형태 검증”)
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].entryDate").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].debitTotal").exists())
                .andExpect(jsonPath("$[0].creditTotal").exists());
    }

    @Test
    @DisplayName("전표 적요 수정(PATCH) 정상: 수정 후 GET하면 description이 변경되어 있다")
    void patch_journalEntry_description_success_then_get_reflects_change() throws Exception {
        // ✅ 1) 생성
        long id = createOneAndReturnId("2025-12-17", "Old description", 10000);

        // ✅ 2) PATCH로 적요 변경
        String patchBody = objectMapper.writeValueAsString(
                Map.of("description", "New description")
        );

        // ⚠️ 구현에 따라 PATCH 응답이 200(본문 있음) / 204(본문 없음) 둘 중 하나일 수 있으니
        // "2xx 성공"으로 느슨하게 받고, 진짜 검증은 다음 GET에서 한다.
        mockMvc.perform(patch("/api/journal-entries/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().is2xxSuccessful());

        // ✅ 3) GET으로 다시 조회해서 description이 바뀌었는지 “결과”를 검증
        mockMvc.perform(get("/api/journal-entries/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.description").value("New description"));
    }
}
