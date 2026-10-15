package com.github.ok200ok.api_todo.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
 * Todo取得・更新成功時のHTTP契約とService連携を検証するWeb層テスト
 * Serviceと所有者ID解決境界をモックし、応答DTOとHTTP境界の振る舞いを確認する
 */
@WebMvcTest
class TodoControllerReadUpdateTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TodoService todoService;

	@MockitoBean
	private OwnerIdResolver ownerIdResolver;

	/**
	 * Todo取得時に所有者IDとTodo IDでServiceを呼び出し、合意済みの応答DTOを返す
	 */
	@Test
	void getsTodoForOwnerAndReturnsResponseDto() throws Exception {
		UUID ownerId = UUID.randomUUID();
		Todo todo = todo(42L, "Read tests", "Check the response", TodoStatusEnum.IN_PROGRESS);
		when(ownerIdResolver.resolveOwnerId()).thenReturn(ownerId);
		when(todoService.findTodoByIdForOwner(42L, ownerId)).thenReturn(todo);

		mockMvc.perform(get("/api/v1/todos/42"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.title").value("Read tests"))
				.andExpect(jsonPath("$.description").value("Check the response"))
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.createdAt").value("2026-10-01T12:30:00Z"))
				.andExpect(jsonPath("$.updatedAt").value("2026-10-02T15:45:00Z"))
				.andExpect(jsonPath("$.user").doesNotExist())
				.andExpect(jsonPath("$.password").doesNotExist());

		verify(todoService).findTodoByIdForOwner(42L, ownerId);
	}

	/**
	 * Todo更新時に所有者ID・Todo ID・置換する全項目をServiceへ渡し、更新後のDTOを返す
	 */
	@Test
	void updatesTodoForOwnerAndReturnsResponseDto() throws Exception {
		UUID ownerId = UUID.randomUUID();
		Todo updatedTodo = todo(42L, "Updated title", "Updated description", TodoStatusEnum.COMPLETED);
		when(ownerIdResolver.resolveOwnerId()).thenReturn(ownerId);
		when(todoService.updateTodoForOwner(
				42L, ownerId, "Updated title", "Updated description", TodoStatusEnum.COMPLETED))
				.thenReturn(updatedTodo);

		mockMvc.perform(put("/api/v1/todos/42")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Updated title\",\"description\":\"Updated description\","
						+ "\"status\":\"COMPLETED\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.title").value("Updated title"))
				.andExpect(jsonPath("$.description").value("Updated description"))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.createdAt").value("2026-10-01T12:30:00Z"))
				.andExpect(jsonPath("$.updatedAt").value("2026-10-02T15:45:00Z"))
				.andExpect(jsonPath("$.user").doesNotExist())
				.andExpect(jsonPath("$.password").doesNotExist());

		verify(todoService).updateTodoForOwner(
				42L, ownerId, "Updated title", "Updated description", TodoStatusEnum.COMPLETED);
	}

	private Todo todo(Long id, String title, String description, TodoStatusEnum status) {
		Todo todo = new Todo(
				new User("alice", "hashed-password", "Alice", "ROLE_USER"),
				title,
				description,
				status);
		ReflectionTestUtils.setField(todo, "id", id);
		ReflectionTestUtils.setField(todo, "createdAt", Instant.parse("2026-10-01T12:30:00Z"));
		ReflectionTestUtils.setField(todo, "updatedAt", Instant.parse("2026-10-02T15:45:00Z"));
		return todo;
	}
}
