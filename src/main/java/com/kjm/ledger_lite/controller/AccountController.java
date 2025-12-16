package com.kjm.ledger_lite.controller;

import com.kjm.ledger_lite.controller.dto.AccountCreateRequest;
import com.kjm.ledger_lite.domain.Account;
import com.kjm.ledger_lite.repository.AccountRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller 역할
 * - HTTP 요청을 "받는" 진입점
 * - 요청(JSON)을 DTO로 변환(@RequestBody)하고 검증(@Valid)한 뒤,
 *   필요한 저장/조회 작업을 Repository에 위임한다.
 *
 * @RestController
 * - 이 클래스의 반환값을 "뷰(html)"가 아니라 "응답 바디(JSON/문자열)"로 보낸다.
 */
@RestController
@RequestMapping("/api/accounts") // 이 컨트롤러의 URL 공통 prefix
public class AccountController {

    /**
     * final
     * - 생성자에서 1번 주입받으면 교체 불가(안정성/명확성)
     * - 스프링이 실행 시점에 AccountRepository "구현체(프록시)"를 자동 생성해서 넣어준다.
     */
    private final AccountRepository accountRepository;

    /**
     * 생성자 주입(DI)
     * - 스프링이 AccountController를 만들 때, 필요한 부품(AccountRepository)을 찾아서 넣어준다.
     * - 여기서 accountRepository는 "인터페이스"지만,
     *   Spring Data JPA가 런타임에 실제 구현 객체를 만들어준다.
     */
    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * 계정과목 등록
     * 요청 흐름:
     * 1) POST /api/accounts 요청 수신
     * 2) JSON body를 AccountCreateRequest로 변환(@RequestBody)
     * 3) @Valid로 DTO 유효성 검증(@NotBlank)
     * 4) 중복검사 후 저장
     * 5) 정상 시 201 Created 반환
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody AccountCreateRequest req) {

        // 비즈니스 룰: 계정과목 코드는 중복되면 안 된다.
        if (accountRepository.findByCode(req.code()).isPresent()) {
            // 지금은 단순 예외로 처리(나중에 400으로 예쁘게 내려주도록 개선할 예정)
            throw new IllegalArgumentException("Account code already exists: " + req.code());
        }

        // 엔티티 생성(=DB에 저장될 데이터 구조)
        Account account = new Account(req.code(), req.name());

        // Repository.save() 호출 → (스프링이 만든 구현체) → JPA/Hibernate → DB INSERT
        accountRepository.save(account);
    }

    /**
     * 계정과목 목록 조회
     * - GET /api/accounts
     * - DB에서 전체 조회 후 JSON 배열로 반환
     */
    @GetMapping
    public List<Account> list() {
        return accountRepository.findAll();
    }
}
