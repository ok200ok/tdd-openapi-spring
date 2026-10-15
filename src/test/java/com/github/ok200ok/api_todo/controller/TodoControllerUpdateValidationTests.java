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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.service.TodoService;

/**
 * Todo更新APIのDTO制約とHTTP入力変換を検証するWeb層テスト
 * Serviceと所有者ID解決境界をモックし、DBや所有者解決方式を持ち込まずHTTP境界を確認する
 */
@WebMvcTest
class TodoControllerUpdateValidationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TodoService todoService;

	@MockitoBean
	private OwnerIdResolver ownerIdResolver;

	/**
	 * 更新時に状態の欠落・不正値と未知JSON項目を拒否し、Serviceを呼び出さない
	 */
	@Test
	void rejectsMissingOrInvalidStatusAndUnknownFields() throws Exception {
		List<String> invalidRequests = List.of(
				"{\"title\":\"Write tests\"}",
				"{\"title\":\"Write tests\",\"status\":\"UNKNOWN\"}",
				"{\"title\":\"Write tests\",\"status\":\"COMPLETED\",\"unexpected\":\"value\"}");

		for (String request : invalidRequests) {
			mockMvc.perform(put("/api/v1/todos/42")
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
		when(ownerIdResolver.resolveOwnerId()).thenReturn(ownerId);
		when(todoService.updateTodoForOwner(
				eq(42L), eq(ownerId), eq(title), eq("details"), eq(TodoStatusEnum.COMPLETED)))
				.thenReturn(todo(42L, title, "details", TodoStatusEnum.COMPLETED));

		mockMvc.perform(put("/api/v1/todos/42")
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
		String longDescription = "x".repeat(5_000);
		when(ownerIdResolver.resolveOwnerId()).thenReturn(ownerId);
		when(todoService.updateTodoForOwner(
				eq(42L), eq(ownerId), eq("Write tests"), eq((String) null), eq(TodoStatusEnum.PENDING)))
				.thenReturn(todo(42L, "Write tests", null, TodoStatusEnum.PENDING));
		when(todoService.updateTodoForOwner(
				eq(42L), eq(ownerId), eq("Write tests"), eq(longDescription), eq(TodoStatusEnum.PENDING)))
				.thenReturn(todo(42L, "Write tests", longDescription, TodoStatusEnum.PENDING));

		mockMvc.perform(put("/api/v1/todos/42")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Write tests\",\"status\":\"PENDING\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/v1/todos/42")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Write tests\",\"description\":null,\"status\":\"PENDING\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/v1/todos/42")
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
