package com.example.demo.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.SignupRequest;
import com.example.demo.auth.response.LoginResponse;
import com.example.demo.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Sign-UP, Login API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(
            summary = "회원가입",
            description = "아이디, 비밀번호, 닉네임을 입력해 새 사용자를 등록합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류 또는 중복 아이디")
    })
    public ResponseEntity<String> signup(
            @RequestBody SignupRequest request
    ) {
        authService.signup(request);
        return ResponseEntity.ok("회원가입 완료");
    }

    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = "로그인에 성공하면 JWT accessToken과 사용자 정보를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치")
    })
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }
}
