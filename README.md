# LedgerLite

> 전표(분개)를 입력·수정할 수 있는 **미니 전표/계정과목 관리 REST API**  

---

## Why this project

- 지원 직무(회계시스템 개발)와 직접 맞닿은 도메인(계정과목/전표/차변·대변 규칙)을 **직접 모델링**해보고 싶었습니다.
- 회계 자격증 기반의 도메인 이해를 개발로 연결해 **업무 규칙을 코드로 검증**하는 경험을 만들었습니다.

---

## Key Features

- **계정과목(Account)**
  - 생성: 중복 코드 방지 (중복 시 **409 Conflict**)
  - 목록 조회
- **전표(Journal Entry)**
  - 생성: **차변합 = 대변합** 규칙을 Service 레이어에서 검증 후 저장 (불일치 시 400)
  - 단건 조회: 라인 + 계정과목(code/name)까지 포함해 반환(DTO)
  - 목록 조회: 전표+라인+계정과목을 한 번에 조회하도록 구성해(N+1 이슈를 피하는 방향) 요약 DTO로 반환
  - 수정(Update): 전표 적요(description)만 부분 변경(PATCH) + 테스트로 검증
- **표준 에러 응답(JSON)**
  - 400/404/409를 상황에 맞게 반환하고, 동일한 포맷으로 응답

---

## Tech Stack

- Java 17
- Spring Boot / Spring Web (REST)
- Spring Data JPA (Hibernate)
- H2 Database
- Validation (jakarta validation)
- Test: Spring Boot Test + JUnit5 + MockMvc

---

## API Endpoints

> Content-Type: `application/json`  
> Error format: 공통 JSON 응답(400/404/409)

### Accounts

- `POST /api/accounts` : 계정과목 생성  
  - ✅ 201 Created  
  - ❌ 400 Bad Request (검증 실패) / ❌ 409 Conflict (중복 코드)
- `GET /api/accounts` : 계정과목 목록 조회  
  - ✅ 200 OK

### Journal Entries

- `POST /api/journal-entries` : 전표 생성(차/대 합계 검증)  
  - ✅ 201 Created (응답 바디에 생성된 id)  
  - ❌ 400 Bad Request (차/대 불일치, dcType 오류 등) / ❌ 404 Not Found (계정과목 없음)
- `GET /api/journal-entries` : 전표 목록 조회(요약: 차/대 합계)  
  - ✅ 200 OK
- `GET /api/journal-entries/{id}` : 전표 단건 조회(라인 + 계정과목 포함)  
  - ✅ 200 OK / ❌ 404 Not Found
- `PATCH /api/journal-entries/{id}` : 전표 적요(description) 수정  
  - ✅ 200 OK / ❌ 400 Bad Request / ❌ 404 Not Found  
  - PATCH는 전표의 “일부 필드(적요)”만 변경하도록 구현하였습니다.

---

## Error Response Format

예시)

```json
{
  "timestamp": "2025-12-18T15:18:55.295569",
  "status": 409,
  "error": "Conflict",
  "message": "Already exists (unique constraint violated)",
  "path": "/api/accounts"
}
```

---

## How to Run

`./gradlew bootRun`

- Base URL: `http://localhost:8080`

--- 

## Test Proof

- MockMvc 기반 통합 테스트 **10건**, 실패 **0건** (100% successful)

- 검증 범위: Controller → Service(@Transactional) → Repository(JPA) → H2 DB

[전체 테스트 결과] <br>

<img width="563" height="90" alt="gradle-test-success png" src="https://github.com/user-attachments/assets/e64cd52b-675f-4a88-b7be-ae7907a306a0" /> <br>

[전체 테스트 결과 요약] <br>

<img width="3087" height="1084" alt="test-report-summary png" src="https://github.com/user-attachments/assets/662629bb-886f-45cd-9aea-e718cc99297a" /> <br>

[Account 테스트 결과] <br>

<img width="3006" height="1123" alt="test-report-account png" src="https://github.com/user-attachments/assets/80153a6e-96fd-4d63-8b5f-2ab7dc560113" /> <br>

[JounalEntry 테스트 결과] <br>

<img width="3802" height="1344" alt="test-report-journal-entry png" src="https://github.com/user-attachments/assets/3d8a07e1-39ed-45bf-b2ed-2a48f91e5c6e" /> <br>

---

## Future Plans

### Soft Delete (전표 삭제 고도화)

- 회계 데이터는 감사/추적 필요성이 높아 **Hard Delete 대신 Soft Delete**로 확장할 계획입니다.
- 구현 방향
  - 전표에 `deletedAt`, `deletedBy`를 추가해 삭제 이력을 보존
  - 기본 조회/목록 API는 `deletedAt is null`만 노출(사용자 화면에서는 “삭제된 전표” 숨김)
  - 필요 시 감사/관리 목적의 조회에서는 삭제 이력 포함(권한 기반으로 분리)

---

## What I learned

- Spring 요청 흐름(DispatcherServlet → Controller → Service → Repository → DB)을 기능 단위로 경험하였습니다.

- 도메인 규칙(차/대 합계 일치, 존재하지 않는 계정 처리 등)을 Service 레이어에서 
  
  검증하였습니다.

- 예외를 상태코드(400/404/409)로 분리하고, 표준 JSON 응답으로 통일하였습니다.

- 테스트로 “재현 가능한 정상동작(10 tests / 0 failures)”을 증명하였습니다.
