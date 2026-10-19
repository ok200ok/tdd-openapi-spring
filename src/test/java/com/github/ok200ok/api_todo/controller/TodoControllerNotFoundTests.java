package com.github.ok200ok.api_todo.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.security.AuthenticatedUserIdResolver;
import com.github.ok200ok.api_todo.security.JwtTokenProvider;
import com.github.ok200ok.api_todo.security.SecurityConfiguration;
import com.github.ok200ok.api_todo.security.TestJwtConfiguration;
import com.github.ok200ok.api_todo.service.TodoNotFoundException;
import com.github.ok200ok.api_todo.service.TodoService;

/**
 * Todoが見つからない場合のHTTPエラー契約を検証するWeb層テスト
 * 認証済み要求でServiceからの例外が共通のProblem Details応答へ変換されることを確認する
 */
@WebMvcTest
@Import({
		SecurityConfiguration.class,
		AuthenticatedUserIdResolver.class,
		JwtTokenProvider.class,
		TestJwtConfiguration.class
})
class TodoControllerNotFoundTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private TodoService todoService;

	/**
	 * 取得対象がない場合に404 Problem Detailsを返し、入力エラー拡張を含めない
	 */
	@Test
	void returnsProblemDetailsWhenTodoCannotBeFoundOnRead() throws Exception {
		UUID ownerId = UUID.randomUUID();
		when(todoService.findTodoByIdForOwner(42L, ownerId))
				.thenThrow(new TodoNotFoundException(42L, ownerId));

		assertNotFoundResponse(get("/api/v1/todos/42")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenProvider.createToken(ownerId)));
	}

	/**
	 * 更新対象がない場合に404 Problem Detailsを返し、入力エラー拡張を含めない
	 */
	@Test
	void returnsProblemDetailsWhenTodoCannotBeFoundOnUpdate() throws Exception {
		UUID ownerId = UUID.randomUUID();
		when(todoService.updateTodoForOwner(
				42L, ownerId, "Updated title", "Updated description", TodoStatusEnum.COMPLETED))
				.thenThrow(new TodoNotFoundException(42L, ownerId));

		assertNotFoundResponse(put("/api/v1/todos/42")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenProvider.createToken(ownerId))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Updated title\",\"description\":\"Updated description\","
						+ "\"status\":\"COMPLETED\"}"));
	}

	/**
	 * 削除対象がない場合に404 Problem Detailsを返し、入力エラー拡張を含めない
	 */
	@Test
	void returnsProblemDetailsWhenTodoCannotBeFoundOnDelete() throws Exception {
		UUID ownerId = UUID.randomUUID();
		doThrow(new TodoNotFoundException(42L, ownerId))
				.when(todoService).deleteTodoForOwner(42L, ownerId);

		assertNotFoundResponse(delete("/api/v1/todos/42")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenProvider.createToken(ownerId)));
	}

	private void assertNotFoundResponse(
			MockHttpServletRequestBuilder request) throws Exception {
		mockMvc.perform(request)
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.errors").doesNotExist());
	}
}
