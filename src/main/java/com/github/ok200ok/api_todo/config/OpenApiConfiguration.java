package com.github.ok200ok.api_todo.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * OpenAPI文書全体に関わる共通設定を配置する構成クラス
 * セキュリティスキームなど複数の操作へ影響する定義をControllerから分離して管理する
 */
@Configuration(proxyBeanMethods = false)
// springdocがOpenAPI文書のcomponents.securitySchemesへbearerAuthを登録する
// HTTPのAuthorizationヘッダーでBearer JWTを送る方式を表し、個々の操作は@SecurityRequirementでこの名前を参照する
@SecurityScheme(
		name = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT")
public class OpenApiConfiguration {
}
