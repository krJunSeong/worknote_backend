package com.example.demo.auth.service;

import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.SignupRequest;
import com.example.demo.auth.jwt.JwtTokenProvider;
import com.example.demo.auth.response.LoginResponse;
import com.example.demo.common.exception.InvalidCredentialsException;
import com.example.demo.user.entity.User;
import com.example.demo.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final int LOGIN_ID_MIN_LENGTH = 4;
    private static final int LOGIN_ID_MAX_LENGTH = 20;
    private static final int SIGNUP_PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 64;
    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 12;
    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}_ ]+$");

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
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {

            throw new InvalidCredentialsException();
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
        validatePattern(
                request.getLoginId().trim(),
                LOGIN_ID_PATTERN,
                "아이디에는 영문, 숫자, 밑줄(_)만 사용할 수 있습니다."
        );

        validateLength(
                request.getPassword(),
                SIGNUP_PASSWORD_MIN_LENGTH,
                PASSWORD_MAX_LENGTH,
                "비밀번호는 8자 이상 64자 이하로 입력해 주세요."
        );

        validateLength(
                request.getNickname().trim(),
                NICKNAME_MIN_LENGTH,
                NICKNAME_MAX_LENGTH,
                "닉네임은 2자 이상 12자 이하로 입력해 주세요."
        );
        validatePattern(
                request.getNickname().trim(),
                NICKNAME_PATTERN,
                "닉네임에는 문자, 숫자, 공백, 밑줄(_)만 사용할 수 있습니다."
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
        validatePattern(
                request.getLoginId().trim(),
                LOGIN_ID_PATTERN,
                "아이디에는 영문, 숫자, 밑줄(_)만 사용할 수 있습니다."
        );

        if (request.getPassword().length() > PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("비밀번호는 64자를 초과할 수 없습니다.");
        }
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

    private void validatePattern(String value, Pattern pattern, String message) {
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(message);
        }
    }
}
