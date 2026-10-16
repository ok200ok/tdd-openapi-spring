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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
class OpenApiDocumentationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TodoService todoService;

	@MockitoBean
	private OwnerIdResolver ownerIdResolver;

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
	 * Swagger UIの公開URLがUI画面へ案内する
	 */
	@Test
	void redirectsSwaggerUiEntryToUiPage() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", containsString("/swagger-ui/index.html")));
	}
}
