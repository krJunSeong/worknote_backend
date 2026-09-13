package com.example.demo.common.exception;

public class InvalidCredentialsException extends RuntimeException {

    public static final String CODE = "INVALID_CREDENTIALS";
    public static final String DEFAULT_MESSAGE = "아이디 또는 비밀번호가 올바르지 않습니다.";

    public InvalidCredentialsException() {
        super(DEFAULT_MESSAGE);
    }
}
