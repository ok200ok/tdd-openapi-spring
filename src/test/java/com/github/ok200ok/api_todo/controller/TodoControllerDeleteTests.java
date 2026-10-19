package com.github.ok200ok.api_todo.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.ok200ok.api_todo.security.AuthenticatedUserIdResolver;
import com.github.ok200ok.api_todo.security.JwtTokenProvider;
import com.github.ok200ok.api_todo.security.SecurityConfiguration;
import com.github.ok200ok.api_todo.security.TestJwtConfiguration;
import com.github.ok200ok.api_todo.service.TodoService;

/**
 * Todo削除APIのHTTP契約とService連携を検証するWeb層テスト
 * Serviceをモックし、認証済みBearer tokenのsubjectを所有者として解決する前提で削除応答を確認する
 */
@WebMvcTest(TodoController.class)
@Import({
		SecurityConfiguration.class,
		AuthenticatedUserIdResolver.class,
		JwtTokenProvider.class,
		TestJwtConfiguration.class
})
class TodoControllerDeleteTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private TodoService todoService;

	/**
	 * 削除成功時に解決済み所有者IDとTodo IDでServiceを呼び、本文のない204を返す
	 */
	@Test
	void deletesTodoForOwnerAndReturnsNoContent() throws Exception {
		UUID ownerId = UUID.randomUUID();

		mockMvc.perform(delete("/api/v1/todos/42")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenProvider.createToken(ownerId)))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		verify(todoService).deleteTodoForOwner(42L, ownerId);
	}
}
