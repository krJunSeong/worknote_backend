package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI workNoteOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("WorkNote API")
                                .description(
                                        "업무일지 CRUD, 인증, AI 분석 기능을 제공하는 WorkNote REST API 문서입니다."
                                )
                                .version("v1")
                                .contact(
                                        new Contact()
                                                .name("WorkNote")
                                )
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        SECURITY_SCHEME_NAME,
                                        new SecurityScheme()
                                                .name(SECURITY_SCHEME_NAME)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description(
                                                        "로그인 API에서 발급받은 accessToken을 입력하세요. Bearer 접두사는 Swagger UI가 자동으로 처리합니다."
                                                )
                                )
                );
    }
}
