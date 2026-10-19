package com.github.ok200ok.api_todo.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.security.AuthenticatedUserIdResolver;
import com.github.ok200ok.api_todo.security.JwtTokenProvider;
import com.github.ok200ok.api_todo.security.SecurityConfiguration;
import com.github.ok200ok.api_todo.security.TestJwtConfiguration;
import com.github.ok200ok.api_todo.service.TodoService;

/**
 * Todo更新APIのDTO制約とHTTP入力変換を検証するWeb層テスト
 * Serviceをモックし、認証済みBearer tokenのsubjectを所有者として解決する前提でHTTP境界を確認する
 */
@WebMvcTest(TodoController.class)
@Import({
		SecurityConfiguration.class,
		AuthenticatedUserIdResolver.class,
		JwtTokenProvider.class,
		TestJwtConfiguration.class
})
class TodoControllerUpdateValidationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private TodoService todoService;

	/**
	 * 更新時に状態の欠落・不正値と未知JSON項目を拒否し、Serviceを呼び出さない
	 */
	@Test
	void rejectsMissingOrInvalidStatusAndUnknownFields() throws Exception {
		String authorization = "Bearer " + jwtTokenProvider.createToken(UUID.randomUUID());
		List<String> invalidRequests = List.of(
				"{\"title\":\"Write tests\"}",
				"{\"title\":\"Write tests\",\"status\":\"UNKNOWN\"}",
				"{\"title\":\"Write tests\",\"status\":\"COMPLETED\",\"unexpected\":\"value\"}");

		for (String request : invalidRequests) {
			mockMvc.perform(put("/api/v1/todos/42")
					.header(HttpHeaders.AUTHORIZATION, authorization)
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
					.andExpect(status().isBadRequest());
		}

		verifyNoInteractions(todoService);
	}

	/**
	 * タイトルを前後空白除去後に検証し、100文字を受け入れて状態とともにServiceへ渡す
	 */
	@Test
	void trimsAndValidatesTitleOnUpdate() throws Exception {
		String title = "a".repeat(100);
		UUID ownerId = UUID.randomUUID();
		String authorization = "Bearer " + jwtTokenProvider.createToken(ownerId);
		when(todoService.updateTodoForOwner(
				eq(42L), eq(ownerId), eq(title), eq("details"), eq(TodoStatusEnum.COMPLETED)))
				.thenReturn(todo(42L, title, "details", TodoStatusEnum.COMPLETED));

		mockMvc.perform(put("/api/v1/todos/42")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"   " + title + "   \",\"description\":\"details\",\"status\":\"COMPLETED\"}"))
				.andExpect(status().isOk());

		verify(todoService).updateTodoForOwner(
				eq(42L), eq(ownerId), eq(title), eq("details"), eq(TodoStatusEnum.COMPLETED));

		for (String invalidTitle : List.of("{\"title\":null}", "{\"title\":\"   \"}",
				"{\"title\":\"" + "a".repeat(101) + "\"}")) {
			String request = invalidTitle.substring(0, invalidTitle.length() - 1)
					+ ",\"status\":\"COMPLETED\"}";
			mockMvc.perform(put("/api/v1/todos/42")
					.header(HttpHeaders.AUTHORIZATION, authorization)
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
					.andExpect(status().isBadRequest());
		}
	}

	/**
	 * 更新で説明を省略またはnullにするとServiceへnullを渡し、長い説明には固定長制約を設けない
	 */
	@Test
	void treatsOmittedAndNullDescriptionAsRemovalAndAllowsLongDescription() throws Exception {
		UUID ownerId = UUID.randomUUID();
		String authorization = "Bearer " + jwtTokenProvider.createToken(ownerId);
		String longDescription = "x".repeat(5_000);
		when(todoService.updateTodoForOwner(
				eq(42L), eq(ownerId), eq("Write tests"), eq((String) null), eq(TodoStatusEnum.PENDING)))
				.thenReturn(todo(42L, "Write tests", null, TodoStatusEnum.PENDING));
		when(todoService.updateTodoForOwner(
				eq(42L), eq(ownerId), eq("Write tests"), eq(longDescription), eq(TodoStatusEnum.PENDING)))
				.thenReturn(todo(42L, "Write tests", longDescription, TodoStatusEnum.PENDING));

		mockMvc.perform(put("/api/v1/todos/42")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Write tests\",\"status\":\"PENDING\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/v1/todos/42")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Write tests\",\"description\":null,\"status\":\"PENDING\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/v1/todos/42")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Write tests\",\"description\":\"" + longDescription
						+ "\",\"status\":\"PENDING\"}"))
				.andExpect(status().isOk());

		verify(todoService, org.mockito.Mockito.times(2)).updateTodoForOwner(
				eq(42L), eq(ownerId), eq("Write tests"), eq((String) null), eq(TodoStatusEnum.PENDING));
		verify(todoService).updateTodoForOwner(
				eq(42L), eq(ownerId), eq("Write tests"), eq(longDescription), eq(TodoStatusEnum.PENDING));
	}

	private Todo todo(Long id, String title, String description, TodoStatusEnum status) {
		Todo todo = new Todo(
				new User("alice", "hashed-password", null, "ROLE_USER"),
				title,
				description,
				status);
		ReflectionTestUtils.setField(todo, "id", id);
		ReflectionTestUtils.setField(todo, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
		ReflectionTestUtils.setField(todo, "updatedAt", Instant.parse("2026-01-02T00:00:00Z"));
		return todo;
	}
}
