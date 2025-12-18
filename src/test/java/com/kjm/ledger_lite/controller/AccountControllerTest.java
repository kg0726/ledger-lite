package com.kjm.ledger_lite.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AccountControllerTest
 *
 * ✅ 목적
 * - /api/accounts 엔드포인트의 주요 동작(C, R)이 정상인지 검증
 *
 * ✅ 흐름(요청→처리→응답)
 * - MockMvc가 HTTP 요청을 시뮬레이션
 * - DispatcherServlet → AccountController → AccountService(@Transactional) → AccountRepository(JPA) → H2 DB
 * - 응답 status / JSON body를 검증
 */
@SpringBootTest
@AutoConfigureMockMvc

/**
 * ✅ 테스트 DB를 분리(mem H2)
 * - 로컬 file DB를 더럽히지 않음
 * - 매 테스트 실행 시 깨끗한 환경에서 재현 가능
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
 * ✅ 테스트 종료 시점에 롤백
 * - 테스트가 DB 상태에 의존하지 않도록 보장
 */
@Transactional
class AccountControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("계정과목 생성 정상: 201 Created")
    void create_account_success_returns201() throws Exception {
        // ✅ 요청 JSON (AccountCreateRequest record와 매핑됨)
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("code", "2000", "name", "TEST_ACCOUNT")
        );

        mockMvc.perform(
                        post("/api/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated());

        // ✅ 생성 후 목록 조회로 실제 반영됐는지 2차 확인(신뢰도↑)
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", Matchers.hasItem("2000")))
                .andExpect(jsonPath("$[*].name", Matchers.hasItem("TEST_ACCOUNT")));
    }

    @Test
    @DisplayName("계정과목 중복 생성: 409 Conflict (unique constraint violated)")
    void create_account_duplicate_returns409() throws Exception {

        // ✅ 1) 먼저 생성
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("code", "3000", "name", "DUPLICATE")
        );

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // ✅ 2) 같은 code로 한 번 더 생성 → 409 Conflict + 표준 에러 응답
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Already exists (unique constraint violated)"))
                .andExpect(jsonPath("$.path").value("/api/accounts"));
    }

    @Test
    @DisplayName("계정과목 목록 조회 정상: 200 OK + JSON 배열")
    void list_accounts_returns200_and_array() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }
}
