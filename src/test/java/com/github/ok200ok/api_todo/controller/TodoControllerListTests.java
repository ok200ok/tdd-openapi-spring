package com.github.ok200ok.api_todo.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.security.JwtTokenProvider;
import com.github.ok200ok.api_todo.security.SecurityConfiguration;
import com.github.ok200ok.api_todo.security.TestJwtConfiguration;
import com.github.ok200ok.api_todo.service.TodoService;

/**
 * Todo一覧APIのページ契約とService連携を検証するWeb層テスト
 * Serviceをモックし、認証済みBearer tokenのsubjectを所有者として解決する前提でページ変換を確認する
 */
@WebMvcTest(TodoController.class)
@Import({
		SecurityConfiguration.class,
		JwtTokenProvider.class,
		TestJwtConfiguration.class
})
class TodoControllerListTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private TodoService todoService;

	/**
	 * クエリ省略時に既定ページをServiceへ渡し、1始まりのページ情報とTodo DTOを返す
	 */
	@Test
	void returnsDefaultTodoPageForOwner() throws Exception {
		UUID ownerId = UUID.randomUUID();
		PageRequest pageable = PageRequest.of(0, 5);
		Todo todo = todo(42L, "List tests", "Check page response", TodoStatusEnum.PENDING);
		Page<Todo> page = new PageImpl<>(List.of(todo), pageable, 7);
		when(todoService.findTodosForOwner(ownerId, pageable)).thenReturn(page);

		mockMvc.perform(get("/api/v1/todos")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenProvider.createToken(ownerId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].id").value(42))
				.andExpect(jsonPath("$.items[0].title").value("List tests"))
				.andExpect(jsonPath("$.items[0].description").value("Check page response"))
				.andExpect(jsonPath("$.items[0].status").value("PENDING"))
				.andExpect(jsonPath("$.items[0].createdAt").value("2026-10-01T12:30:00Z"))
				.andExpect(jsonPath("$.items[0].updatedAt").value("2026-10-02T15:45:00Z"))
				.andExpect(jsonPath("$.items[0].user").doesNotExist())
				.andExpect(jsonPath("$.items[0].password").doesNotExist())
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(5))
				.andExpect(jsonPath("$.totalElements").value(7))
				.andExpect(jsonPath("$.totalPages").value(2));

		verify(todoService).findTodosForOwner(ownerId, pageable);
	}

	/**
	 * 1始まりの公開ページを0始まりへ変換してServiceへ渡し、ページ応答へ反映する
	 */
	@Test
	void convertsRequestedPageToZeroBasedServicePage() throws Exception {
		UUID ownerId = UUID.randomUUID();
		PageRequest pageable = PageRequest.of(1, 3);
		Todo todo = todo(41L, "Second page", null, TodoStatusEnum.IN_PROGRESS);
		Todo anotherTodo = todo(40L, "Another second-page item", null, TodoStatusEnum.PENDING);
		Page<Todo> page = new PageImpl<>(List.of(todo, anotherTodo), pageable, 5);
		when(todoService.findTodosForOwner(ownerId, pageable)).thenReturn(page);

		mockMvc.perform(get("/api/v1/todos")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenProvider.createToken(ownerId))
				.queryParam("page", "2").queryParam("size", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].id").value(41))
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.page").value(2))
				.andExpect(jsonPath("$.size").value(3))
				.andExpect(jsonPath("$.totalElements").value(5))
				.andExpect(jsonPath("$.totalPages").value(2));

		verify(todoService).findTodosForOwner(ownerId, pageable);
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
