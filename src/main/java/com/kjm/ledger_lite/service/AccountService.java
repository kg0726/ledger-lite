package com.kjm.ledger_lite.service;

import com.kjm.ledger_lite.controller.dto.AccountCreateRequest;
import com.kjm.ledger_lite.domain.Account;
import com.kjm.ledger_lite.repository.AccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자로부터 입력을 받아, DB에 계정과목을 생성
 */
@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;

    }

    // 계정과목 생성 메서드
    @Transactional
    public void create(AccountCreateRequest req) {
        // 계정과목 코드는 중복될 수 없음
        if (accountRepository.findByCode(req.code()).isPresent()) {
            throw new DataIntegrityViolationException("Account code already exists: " + req.code());
        }

        Account account = new Account(req.code(), req.name());
        accountRepository.save(account);
    }

    // 전체 계정과목 조회 메서드
    @Transactional(readOnly = true)
    public List<Account> list() {
        return accountRepository.findAll();
    }
}
