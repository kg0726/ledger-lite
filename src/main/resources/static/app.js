/*
  app.js

  ✅ 역할
  - 화면 이벤트(버튼 클릭)를 받아서 REST API를 호출(fetch = AJAX)
  - 응답(JSON)을 화면에 렌더링
  - 에러(ApiErrorResponse)면 message/status를 화면에 표시
  - 계정과목(Account) 변경 시, 전표 라인 드롭다운을 자동 갱신
  - 전표 입력 중 차변/대변 합계를 실시간으로 보여주는 UX 개선

  ✅ 전체 흐름(대표 예: 전표 등록)
  1) 사용자가 전표 입력 → [전표 등록] 클릭
  2) JS가 body(JSON) 구성 → POST /api/journal-entries 호출
  3) 성공(201) → 생성 id 반환 → 메시지 표시 + 바로 GET 조회해서 화면에 출력
  4) 실패(400/404) → ApiErrorResponse를 파싱해서 message를 표시
*/

// =======================
// 0) DOM 요소 캐싱
// =======================

// 공통 출력 영역
const msgBox = document.querySelector("#msgBox");
const accountsBox = document.querySelector("#accountsBox");
const entryBox = document.querySelector("#entryBox");

// Account 입력
const accCode = document.querySelector("#accCode");
const accName = document.querySelector("#accName");
const btnCreateAccount = document.querySelector("#btnCreateAccount");
const btnReloadAccounts = document.querySelector("#btnReloadAccounts");

// JournalEntry 입력
const jeDate = document.querySelector("#jeDate");
const jeDesc = document.querySelector("#jeDesc");
const linesTbody = document.querySelector("#linesTbody");
const btnAddLine = document.querySelector("#btnAddLine");
const btnCreateEntry = document.querySelector("#btnCreateEntry");

// JournalEntry 조회
const jeId = document.querySelector("#jeId");
const btnGetEntry = document.querySelector("#btnGetEntry");

// (선택) 합계 표시 영역: index.html에 없을 수도 있으니 null 방어
const debitSumEl = document.querySelector("#debitSum");
const creditSumEl = document.querySelector("#creditSum");
const diffSumEl = document.querySelector("#diffSum");

// ✅ 전역 상태: 계정과목 목록을 메모리에 저장(라인 select 옵션 구성에 사용)
let accounts = [];

// =======================
// 1) 유틸(메시지/응답 처리)
// =======================

/**
 * showMessage(value)
 *
 * ✅ 역할
 * - 화면 상단 msgBox에 문자열 또는 JSON을 보기 좋게 표시한다.
 */
function showMessage(value) {
  if (!msgBox) return;

  msgBox.textContent =
    typeof value === "string" ? value : JSON.stringify(value, null, 2);
}

/**
 * handleResponse(res)
 *
 * ✅ 역할
 * - fetch 응답을 공통 처리한다.
 * - 성공(res.ok=true)이면 JSON(또는 text)을 반환
 * - 실패(res.ok=false)이면 에러 내용을 msgBox에 찍고 throw 한다.
 *
 * ✅ 왜 text()로 먼저 받나?
 * - 서버가 숫자만 반환(예: 1)해도 JSON으로 파싱 가능
 * - 혹시 JSON이 아닌 text가 오더라도 깨지지 않게 하기 위함
 */
async function handleResponse(res) {
  const text = await res.text();
  let data = null;

  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  // ✅ 에러(400/404 등)면 ApiErrorResponse를 msgBox에 표시하고 예외 발생
  if (!res.ok) {
    showMessage(data || { status: res.status, error: "Request failed" });
    throw new Error("Request failed");
  }

  return data;
}

// =======================
// 2) UX 개선: 차변/대변 합계 실시간 계산
// =======================

/**
 * updateSums()
 *
 * ✅ 역할
 * - 현재 화면의 분개 라인 입력값으로 차변합/대변합/차이를 계산해 표시한다.
 *
 * ✅ 주의
 * - sumBox UI를 아직 index.html에 안 넣었으면 debitSumEl 등이 null일 수 있으니 방어한다.
 */
function updateSums() {
  // 합계 표시 UI가 없으면(아직 미적용) 계산만 생략
  if (!debitSumEl || !creditSumEl || !diffSumEl) return;

  const rows = Array.from(linesTbody.querySelectorAll("tr"));
  let debit = 0;
  let credit = 0;

  rows.forEach((tr) => {
    const selects = tr.querySelectorAll("select");
    const dcSelect = selects[0]; // 0번: 차/대
    const amountInput = tr.querySelector("input"); // 금액 input

    const dcType = dcSelect?.value;
    const amount = Number(amountInput?.value) || 0;

    if (dcType === "DEBIT") debit += amount;
    if (dcType === "CREDIT") credit += amount;
  });

  debitSumEl.textContent = String(debit);
  creditSumEl.textContent = String(credit);
  diffSumEl.textContent = String(debit - credit);
}

// =======================
// 3) Account select 옵션 갱신(핵심 UX)
// =======================

/**
 * buildAccountOptions(selectEl, keepSelectedId)
 *
 * ✅ 역할
 * - accounts 배열 기준으로 <select> 옵션을 재생성한다.
 * - 기존 선택값(keepSelectedId)이 있으면 최대한 유지한다.
 *
 * ✅ 왜 필요?
 * - 계정과목을 추가하면 accounts는 최신이 되지만,
 *   이미 만들어진 라인 select 옵션은 자동 갱신되지 않음.
 */
function buildAccountOptions(selectEl, keepSelectedId) {
  selectEl.innerHTML = "";

  // 계정이 없다면 안내 옵션 1개
  if (accounts.length === 0) {
    const opt = document.createElement("option");
    opt.value = "";
    opt.textContent = "계정과목이 없습니다";
    selectEl.appendChild(opt);
    return;
  }

  // 최신 accounts로 옵션 구성
  accounts.forEach((a) => {
    const opt = document.createElement("option");
    opt.value = a.id;
    opt.textContent = `${a.code} - ${a.name} (id:${a.id})`;
    selectEl.appendChild(opt);
  });

  // 기존 선택 유지 시도
  if (keepSelectedId) {
    selectEl.value = String(keepSelectedId);
  }

  // 유지 실패(없던 id)라면 첫 번째로
  if (!selectEl.value) {
    selectEl.value = String(accounts[0].id);
  }
}

/**
 * refreshAccountSelects()
 *
 * ✅ 역할
 * - 현재 화면에 존재하는 모든 라인(row)을 순회하며,
 *   계정과목 select 옵션을 최신 accounts로 갱신한다.
 *
 * ✅ 언제 호출?
 * - loadAccounts()가 성공해서 accounts 배열이 갱신된 직후
 */
function refreshAccountSelects() {
  const rows = Array.from(linesTbody.querySelectorAll("tr"));

  rows.forEach((tr) => {
    const selects = tr.querySelectorAll("select");
    const accSelect = selects[1]; // 1번: 계정과목

    if (!accSelect) return;

    const currentSelectedId = accSelect.value;
    buildAccountOptions(accSelect, currentSelectedId);
  });
}

// =======================
// 4) API: 계정과목 목록/생성
// =======================

/**
 * loadAccounts()
 *
 * ✅ 역할
 * - GET /api/accounts 호출 → accounts 배열 갱신 → 화면 출력
 * - 갱신 직후 refreshAccountSelects() 호출해서 라인 select도 최신화
 */
async function loadAccounts() {
  showMessage("계정과목 목록 로딩 중...");

  const res = await fetch("/api/accounts", { method: "GET" });
  const data = await handleResponse(res);

  accounts = Array.isArray(data) ? data : [];
  if (accountsBox) {
    accountsBox.textContent = JSON.stringify(accounts, null, 2);
  }

  // ✅ 핵심: 계정 목록이 최신이 되었으니, 전표 라인의 드롭다운도 최신화
  refreshAccountSelects();

  showMessage("계정과목 목록 로딩 완료");
}

/**
 * createAccount()
 *
 * ✅ 역할
 * - POST /api/accounts로 계정과목을 생성한다.
 * - 성공하면 loadAccounts()로 목록/드롭다운을 즉시 최신화한다.
 */
async function createAccount() {
  if (!accCode.value.trim() || !accName.value.trim()) {
    showMessage("코드/이름은 공백일 수 없습니다.");
    return;
  }

  const body = {
    code: accCode.value.trim(),
    name: accName.value.trim(),
  };

  const res = await fetch("/api/accounts", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  await handleResponse(res);

  // ✅ 여기서 "성공" 메시지를 띄우지만,
  // 바로 loadAccounts()가 showMessage를 덮어쓸 수 있음(정상 동작)
  showMessage("계정과목 생성 성공(201)");

  accCode.value = "";
  accName.value = "";

  // ✅ 생성 후 즉시 목록/드롭다운 갱신
  await loadAccounts();
}

// =======================
// 5) 전표 라인 UI 생성/초기화
// =======================

/**
 * makeLineRow(defaultDcType)
 *
 * ✅ 역할
 * - 분개 라인 1줄(tr)을 생성한다.
 * - 차/대 select, 금액 input, 계정과목 select, 삭제 버튼을 포함한다.
 * - dcType/amount 변경 시 updateSums()가 호출되도록 이벤트를 연결한다.
 */
function makeLineRow(defaultDcType = "DEBIT") {
  const tr = document.createElement("tr");

  // --- (1) 차/대 select ---
  const tdDc = document.createElement("td");
  const selDc = document.createElement("select");

  ["DEBIT", "CREDIT"].forEach((v) => {
    const opt = document.createElement("option");
    opt.value = v;
    opt.textContent = v;
    if (v === defaultDcType) opt.selected = true;
    selDc.appendChild(opt);
  });

  // ✅ 차/대 변경 시 합계 재계산(UX)
  selDc.addEventListener("change", updateSums);

  tdDc.appendChild(selDc);

  // --- (2) 금액 input ---
  const tdAmt = document.createElement("td");
  const inpAmt = document.createElement("input");
  inpAmt.type = "number";
  inpAmt.placeholder = "예: 10000";
  inpAmt.min = "0";

  // ✅ 금액 입력 시 합계 재계산(UX)
  inpAmt.addEventListener("input", updateSums);

  tdAmt.appendChild(inpAmt);

  // --- (3) 계정과목 select ---
  const tdAcc = document.createElement("td");
  const selAcc = document.createElement("select");

  // ✅ 현재 accounts 기준으로 옵션 구성
  // (계정이 아직 없으면 "계정과목이 없습니다" 옵션이 들어감)
  buildAccountOptions(selAcc, null);

  tdAcc.appendChild(selAcc);

  // --- (4) 삭제 버튼 ---
  const tdDel = document.createElement("td");
  const btnDel = document.createElement("button");
  btnDel.type = "button";
  btnDel.textContent = "삭제";
  btnDel.style.background = "#b83280";

  btnDel.addEventListener("click", () => {
    tr.remove();
    // ✅ 삭제 후 합계 재계산
    updateSums();
  });

  tdDel.appendChild(btnDel);

  // --- 조립 ---
  tr.appendChild(tdDc);
  tr.appendChild(tdAmt);
  tr.appendChild(tdAcc);
  tr.appendChild(tdDel);

  return tr;
}

/**
 * initLines()
 *
 * ✅ 역할
 * - 전표 입력 시작 시 기본 라인 2개(차/대)를 만들어준다.
 * - 생성 직후 updateSums()로 합계도 초기 표시한다.
 */
function initLines() {
  linesTbody.innerHTML = "";
  linesTbody.appendChild(makeLineRow("DEBIT"));
  linesTbody.appendChild(makeLineRow("CREDIT"));

  // ✅ 초기 합계 계산
  updateSums();
}

// =======================
// 6) API: 전표 생성/조회
// =======================

/**
 * createJournalEntry()
 *
 * ✅ 역할
 * - 화면 입력값으로 전표 등록 요청(JSON)을 구성해서 POST /api/journal-entries 호출
 * - 성공 시 생성된 id를 받아 jeId 입력칸에 넣고, 바로 GET 조회까지 수행한다.
 */
async function createJournalEntry() {
  if (!jeDate.value.trim() || !jeDesc.value.trim()) {
    showMessage("전표일자/설명은 공백일 수 없습니다.");
    return;
  }

  const rows = Array.from(linesTbody.querySelectorAll("tr"));
  if (rows.length < 2) {
    showMessage("분개 라인은 최소 2줄 이상 권장(차/대 한 줄씩).");
    return;
  }

  // ✅ 라인 데이터 수집
  const lines = rows.map((tr) => {
    const selects = tr.querySelectorAll("select");
    const dcType = selects[0].value; // 0: 차/대
    const amount = Number(tr.querySelector("input").value);
    const accountId = Number(selects[1].value); // 1: 계정과목

    return { dcType, amount, accountId };
  });

  // ✅ amount 검증(UX)
  if (lines.some((l) => !Number.isFinite(l.amount) || l.amount <= 0)) {
    showMessage("금액(amount)은 0보다 큰 숫자여야 합니다.");
    return;
  }

  // ✅ accountId 검증(계정이 하나도 없으면 NaN/0일 수 있음)
  if (lines.some((l) => !Number.isFinite(l.accountId) || l.accountId <= 0)) {
    showMessage("계정과목을 선택하세요(계정 목록이 비어있을 수 있음).");
    return;
  }

  const body = {
    entryDate: jeDate.value.trim(),
    description: jeDesc.value.trim(),
    lines,
  };

  const res = await fetch("/api/journal-entries", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  const data = await handleResponse(res);

  // ✅ 서버가 숫자(1)만 줄 수도 있고, {id:1} 형태일 수도 있어 둘 다 처리
  const createdId = typeof data === "number" ? data : data?.id;

  showMessage({
    message: "전표 생성 성공(201)",
    createdId,
    requestBody: body,
  });

  // ✅ 생성된 id를 조회칸에 자동 입력 + 바로 조회(UX)
  if (createdId) {
    jeId.value = String(createdId);
    await getJournalEntry();
  }
}

/**
 * getJournalEntry()
 *
 * ✅ 역할
 * - GET /api/journal-entries/{id} 호출 → 응답(JSON)을 entryBox에 출력
 */
async function getJournalEntry() {
  const id = jeId.value.trim();
  if (!id) {
    showMessage("조회할 전표 ID를 입력하세요.");
    return;
  }

  const res = await fetch(`/api/journal-entries/${id}`, { method: "GET" });
  const data = await handleResponse(res);

  if (entryBox) {
    entryBox.textContent = JSON.stringify(data, null, 2);
  }

  showMessage(`전표 조회 성공(200). id=${id}`);
}

// =======================
// 7) 이벤트 바인딩
// =======================

btnCreateAccount.addEventListener("click", createAccount);
btnReloadAccounts.addEventListener("click", loadAccounts);

btnAddLine.addEventListener("click", () => {
  linesTbody.appendChild(makeLineRow("DEBIT"));
  updateSums(); // ✅ 라인 추가 후 합계 재계산
});

btnCreateEntry.addEventListener("click", createJournalEntry);
btnGetEntry.addEventListener("click", getJournalEntry);

// =======================
// 8) 초기 로딩(페이지 열렸을 때 자동 실행)
// =======================

(async function init() {
  try {
    // 1) 계정 목록 로드(→ accounts 갱신)
    await loadAccounts();

    // 2) 기본 라인 2개 생성(→ accounts 기준으로 select 옵션도 채워짐)
    initLines();

    showMessage("초기화 완료: 계정 목록 로드 + 전표 입력 준비 완료");
  } catch (e) {
    // 초기 로딩 실패해도 화면은 떠야 하므로 메시지만 표시
    showMessage("초기 로딩 중 오류. 서버 실행 상태와 API 경로를 확인하세요.");
  }
})();
