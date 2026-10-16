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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.service.TodoNotFoundException;
import com.github.ok200ok.api_todo.service.TodoService;

/**
 * Todoが見つからない場合のHTTPエラー契約を検証するWeb層テスト
 * Serviceからの例外が共通のProblem Details応答へ変換されることを確認する
 */
@WebMvcTest
class TodoControllerNotFoundTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TodoService todoService;

	@MockitoBean
	private OwnerIdResolver ownerIdResolver;

	/**
	 * 取得対象がない場合に404 Problem Detailsを返し、入力エラー拡張を含めない
	 */
	@Test
	void returnsProblemDetailsWhenTodoCannotBeFoundOnRead() throws Exception {
		UUID ownerId = UUID.randomUUID();
		when(ownerIdResolver.resolveOwnerId()).thenReturn(ownerId);
		when(todoService.findTodoByIdForOwner(42L, ownerId))
				.thenThrow(new TodoNotFoundException(42L, ownerId));

		assertNotFoundResponse(get("/api/v1/todos/42"));
	}

	/**
	 * 更新対象がない場合に404 Problem Detailsを返し、入力エラー拡張を含めない
	 */
	@Test
	void returnsProblemDetailsWhenTodoCannotBeFoundOnUpdate() throws Exception {
		UUID ownerId = UUID.randomUUID();
		when(ownerIdResolver.resolveOwnerId()).thenReturn(ownerId);
		when(todoService.updateTodoForOwner(
				42L, ownerId, "Updated title", "Updated description", TodoStatusEnum.COMPLETED))
				.thenThrow(new TodoNotFoundException(42L, ownerId));

		assertNotFoundResponse(put("/api/v1/todos/42")
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
		when(ownerIdResolver.resolveOwnerId()).thenReturn(ownerId);
		doThrow(new TodoNotFoundException(42L, ownerId))
				.when(todoService).deleteTodoForOwner(42L, ownerId);

		assertNotFoundResponse(delete("/api/v1/todos/42"));
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
