package com.example.demo.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.SignupRequest;
import com.example.demo.auth.jwt.JwtTokenProvider;
import com.example.demo.auth.response.LoginResponse;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void signup(SignupRequest request) {

        validateSignupRequest(request);

        String loginId = request.getLoginId().trim();

        if (userRepository.findByLoginId(loginId)
                .isPresent()) {

            throw new IllegalArgumentException(
                    "이미 사용 중인 아이디입니다."
            );
        }

        User user = User.builder()
                .loginId(loginId)
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .nickname(
                        request.getNickname().trim()
                )
                .build();

        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {

        validateLoginRequest(request);

        String loginId = request.getLoginId().trim();

        User user = userRepository
                .findByLoginId(loginId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "아이디 또는 비밀번호가 올바르지 않습니다."
                        )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {

            throw new IllegalArgumentException(
                    "아이디 또는 비밀번호가 올바르지 않습니다."
            );
        }

        String accessToken =
                jwtTokenProvider.createToken(
                        user.getId(),
                        user.getLoginId()
                );

        return LoginResponse.builder()
                .userId(user.getId())
                .loginId(user.getLoginId())
                .nickname(user.getNickname())
                .accessToken(accessToken)
                .tokenType("Bearer")
                .build();
    }

    private void validateSignupRequest(
            SignupRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "요청 데이터가 없습니다."
            );
        }

        if (request.getLoginId() == null
                || request.getLoginId().isBlank()) {

            throw new IllegalArgumentException(
                    "아이디를 입력해 주세요."
            );
        }

        if (request.getPassword() == null
                || request.getPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "비밀번호를 입력해 주세요."
            );
        }

        if (request.getNickname() == null
                || request.getNickname().isBlank()) {

            throw new IllegalArgumentException(
                    "닉네임을 입력해 주세요."
            );
        }
    }

    private void validateLoginRequest(
            LoginRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "요청 데이터가 없습니다."
            );
        }

        if (request.getLoginId() == null
                || request.getLoginId().isBlank()) {

            throw new IllegalArgumentException(
                    "아이디를 입력해 주세요."
            );
        }

        if (request.getPassword() == null
                || request.getPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "비밀번호를 입력해 주세요."
            );
        }
    }
}