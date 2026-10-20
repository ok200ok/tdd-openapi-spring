package com.github.ok200ok.api_todo.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.ok200ok.api_todo.config.OpenApiConfiguration;
import com.github.ok200ok.api_todo.security.AuthenticatedUserIdResolver;
import com.github.ok200ok.api_todo.service.AuthService;
import com.github.ok200ok.api_todo.service.TodoService;

/**
 * OpenAPI文書とSwagger UIのHTTP境界を検証するWeb層テスト
 * Todoの業務処理をモックし、仕様生成とUIの公開に検証を絞る
 */
// Web MVCスライスではライブラリ自動設定が除外されるため、検証対象のspringdoc設定だけを明示的に追加する
@WebMvcTest
@EnableConfigurationProperties({
		SpringDocConfigProperties.class,
		SwaggerUiConfigProperties.class,
		SwaggerUiOAuthProperties.class
})
@ImportAutoConfiguration({
		SpringDocConfiguration.class,
		SpringDocWebMvcConfiguration.class,
		MultipleOpenApiSupportConfiguration.class,
		SwaggerConfig.class
})
@Import({ AuthenticatedUserIdResolver.class, OpenApiConfiguration.class })
class OpenApiDocumentationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TodoService todoService;

	// 全Controllerを読み込むため、認証Controllerの依存をモックしてOpenAPI生成対象だけに検証を絞る
	@MockitoBean
	private AuthService authService;

	/**
	 * OpenAPI文書にTodo APIの操作、DTOスキーマ、エラー応答を含める
	 */
	@Test
	void publishesTodoApiOperationsSchemasAndErrorResponses() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.paths['/api/v1/todos'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/todos'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/todos/{todoId}'].get").exists())
				.andExpect(jsonPath("$.paths['/api/v1/todos/{todoId}'].put").exists())
				.andExpect(jsonPath("$.paths['/api/v1/todos/{todoId}'].delete").exists())
				.andExpect(jsonPath("$.paths['/api/v1/todos'].get.parameters[*].name").value(hasItems("page", "size")))
				.andExpect(jsonPath("$.paths['/api/v1/todos'].post.responses['201']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/todos'].post.responses['400']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/todos/{todoId}'].get.responses['404']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/todos/{todoId}'].put.responses['400']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/todos/{todoId}'].put.responses['404']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/todos/{todoId}'].delete.responses['204']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/todos/{todoId}'].delete.responses['404']").exists())
				.andExpect(jsonPath("$.components.schemas.CreateTodoRequest.properties.title").exists())
				.andExpect(jsonPath("$.components.schemas.CreateTodoRequest.properties.description").exists())
				.andExpect(jsonPath("$.components.schemas.UpdateTodoRequest.properties.title").exists())
				.andExpect(jsonPath("$.components.schemas.UpdateTodoRequest.properties.description").exists())
				.andExpect(jsonPath("$.components.schemas.UpdateTodoRequest.properties.status").exists())
				.andExpect(jsonPath("$.components.schemas.TodoResponse.properties.id").exists())
				.andExpect(jsonPath("$.components.schemas.TodoResponse.properties.title").exists())
				.andExpect(jsonPath("$.components.schemas.TodoResponse.properties.description").exists())
				.andExpect(jsonPath("$.components.schemas.TodoResponse.properties.status").exists())
				.andExpect(jsonPath("$.components.schemas.TodoResponse.properties.createdAt").exists())
				.andExpect(jsonPath("$.components.schemas.TodoResponse.properties.updatedAt").exists())
				.andExpect(jsonPath("$.components.schemas.TodoResponse.properties.user").doesNotExist())
				.andExpect(jsonPath("$.components.schemas.TodoResponse.properties.password").doesNotExist())
				.andExpect(jsonPath("$.components.schemas.TodoPageResponse.properties.items").exists())
				.andExpect(jsonPath("$.components.schemas.TodoPageResponse.properties.page").exists())
				.andExpect(jsonPath("$.components.schemas.TodoPageResponse.properties.size").exists())
				.andExpect(jsonPath("$.components.schemas.TodoPageResponse.properties.totalElements").exists())
				.andExpect(jsonPath("$.components.schemas.TodoPageResponse.properties.totalPages").exists());
	}

	/**
	 * OpenAPI文書に認証APIの操作、エラー応答、入出力スキーマを含め、機密情報を公開しない
	 */
	@Test
	void publishesAuthApiOperationsAndSchemas() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.paths['/api/v1/auth/register'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.responses['201']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.responses['400']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.responses['409']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['200']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['400']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['401']").exists())
				.andExpect(jsonPath("$.components.schemas.RegisterRequest.properties.userName").exists())
				.andExpect(jsonPath("$.components.schemas.RegisterRequest.properties.password").exists())
				.andExpect(jsonPath("$.components.schemas.RegisterRequest.properties.displayName").exists())
				.andExpect(jsonPath("$.components.schemas.LoginRequest.properties.userName").exists())
				.andExpect(jsonPath("$.components.schemas.LoginRequest.properties.password").exists())
				.andExpect(jsonPath("$.components.schemas.RegisterResponse.properties.userName").exists())
				.andExpect(jsonPath("$.components.schemas.RegisterResponse.properties.displayName").exists())
				.andExpect(jsonPath("$.components.schemas.RegisterResponse.properties.accessToken").exists())
				.andExpect(jsonPath("$.components.schemas.RegisterResponse.properties.tokenType").exists())
				.andExpect(jsonPath("$.components.schemas.RegisterResponse.properties.expiresIn").exists())
				.andExpect(jsonPath("$.components.schemas.RegisterResponse.properties.password").doesNotExist())
				.andExpect(jsonPath("$.components.schemas.RegisterResponse.properties.role").doesNotExist())
				.andExpect(jsonPath("$.components.schemas.RegisterResponse.properties.id").doesNotExist())
				.andExpect(jsonPath("$.components.schemas.TokenResponse.properties.accessToken").exists())
				.andExpect(jsonPath("$.components.schemas.TokenResponse.properties.tokenType").exists())
				.andExpect(jsonPath("$.components.schemas.TokenResponse.properties.expiresIn").exists())
				.andExpect(jsonPath("$.components.schemas.TokenResponse.properties.password").doesNotExist())
				.andExpect(jsonPath("$.components.schemas.TokenResponse.properties.role").doesNotExist())
				.andExpect(jsonPath("$.components.schemas.TokenResponse.properties.id").doesNotExist());
	}

	/**
	 * OpenAPI文書にBearer認証スキームを定義し、Todo操作だけへ適用する
	 */
	@Test
	void definesBearerSecuritySchemeAndAppliesItToTodoOperations() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
				.andExpect(jsonPath("$.paths['/api/v1/todos'].get.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/api/v1/todos'].post.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/api/v1/todos/{todoId}'].get.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/api/v1/todos/{todoId}'].put.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/api/v1/todos/{todoId}'].delete.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.security").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security").doesNotExist());
	}

	/**
	 * Swagger UIの公開URLがUI画面へ案内する
	 */
	@Test
	void redirectsSwaggerUiEntryToUiPage() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", containsString("/swagger-ui/index.html")));
	}
}
