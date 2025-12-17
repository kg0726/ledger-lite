package com.kjm.ledger_lite.exceiption;

/**
 * 존재하지 않는 리소스 조회 시 사용할 예외
 * 요청 형식은 맞지만 리소스가 없는 상황에서 응답할 404 예외
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
