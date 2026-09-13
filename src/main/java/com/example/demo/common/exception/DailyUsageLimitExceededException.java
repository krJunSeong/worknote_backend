package com.example.demo.common.exception;

public class DailyUsageLimitExceededException extends RuntimeException {

    public static final String CODE = "DAILY_AI_LIMIT_EXCEEDED";
    public static final String DEFAULT_MESSAGE =
            "오늘 쓸 수 있는 AI기능을 다 썼습니다. 내일 다시 시도해주세요.";

    public DailyUsageLimitExceededException() {
        super(DEFAULT_MESSAGE);
    }
}
