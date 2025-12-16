package com.kjm.ledger_lite.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * AccountCreateRequest (요청 DTO)
 *
 * ✅ 역할
 * - 클라이언트가 보내는 JSON 요청 body를 "안전하게" 받는 객체
 *
 * ✅ 왜 엔티티(Account)로 바로 안 받나?
 * - 엔티티는 DB 구조와 강하게 묶여있어서 API 계약이 흔들릴 수 있고,
 * - 원치 않는 필드까지 클라이언트가 조작하게 될 위험이 생김(통제/보안 문제)
 *
 * ✅ record란?
 * - 자바 16+ 문법
 * - DTO처럼 "데이터만 들고 다니는 객체"를 간단히 만들 때 사용
 *
 * record가 자동으로 만들어주는 것:
 * - 생성자
 * - 접근자: req.code(), req.name()
 * - toString/equals/hashCode
 */
public record AccountCreateRequest(
        @NotBlank String code, // null/""/"   "이면 검증에서 걸러짐(@Valid로 실행됨)
        @NotBlank String name
) {}
