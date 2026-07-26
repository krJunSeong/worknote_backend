package com.example.demo.auth.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    private Long userId;
    private String loginId;
    private String nickname;
    private String accessToken;
    private String tokenType;
}