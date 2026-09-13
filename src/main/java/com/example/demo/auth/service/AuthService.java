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

    private static final int LOGIN_ID_MIN_LENGTH = 4;
    private static final int LOGIN_ID_MAX_LENGTH = 20;
    private static final int PASSWORD_MIN_LENGTH = 5;
    private static final int PASSWORD_MAX_LENGTH = 12;
    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 12;

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

        validateLength(
                request.getLoginId().trim(),
                LOGIN_ID_MIN_LENGTH,
                LOGIN_ID_MAX_LENGTH,
                "아이디는 4자 이상 20자 이하로 입력해 주세요."
        );

        validateLength(
                request.getPassword(),
                PASSWORD_MIN_LENGTH,
                PASSWORD_MAX_LENGTH,
                "비밀번호는 5자 이상 12자 이하로 입력해 주세요."
        );

        validateLength(
                request.getNickname().trim(),
                NICKNAME_MIN_LENGTH,
                NICKNAME_MAX_LENGTH,
                "닉네임은 2자 이상 12자 이하로 입력해 주세요."
        );
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

        validateLength(
                request.getLoginId().trim(),
                LOGIN_ID_MIN_LENGTH,
                LOGIN_ID_MAX_LENGTH,
                "아이디는 4자 이상 20자 이하로 입력해 주세요."
        );

        validateLength(
                request.getPassword(),
                PASSWORD_MIN_LENGTH,
                PASSWORD_MAX_LENGTH,
                "비밀번호는 5자 이상 12자 이하로 입력해 주세요."
        );
    }

    private void validateLength(
            String value,
            int minLength,
            int maxLength,
            String message
    ) {
        int length = value.length();

        if (length < minLength || length > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }
}
