package com.github.ok200ok.api_todo.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.hamcrest.Matchers;
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
 * Todo作成APIの成功時HTTP契約、Service連携、出力DTOを検証するWeb層テスト
 * Serviceをモックし、認証済みBearer tokenのsubjectを所有者として解決する前提でHTTP境界を確認する
 */
@WebMvcTest
@Import({
		SecurityConfiguration.class,
		AuthenticatedUserIdResolver.class,
		JwtTokenProvider.class,
		TestJwtConfiguration.class
})
class TodoControllerCreateTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private TodoService todoService;

	/**
	 * 作成成功時に201、正しいLocation、Entityを公開しないDTOを返し、所有者と入力値をServiceへ渡す
	 */
	@Test
	void createsTodoAndReturnsLocationAndResponseDto() throws Exception {
		UUID ownerId = UUID.randomUUID();
		String title = "Write tests";
		String description = "Cover the HTTP contract";
		Instant createdAt = Instant.parse("2026-10-01T12:30:00Z");
		Instant updatedAt = Instant.parse("2026-10-02T15:45:00Z");
		Todo createdTodo = new Todo(
				new User("alice", "hashed-password", "Alice", "ROLE_USER"),
				title,
				description,
				TodoStatusEnum.PENDING);
		ReflectionTestUtils.setField(createdTodo, "id", 42L);
		ReflectionTestUtils.setField(createdTodo, "createdAt", createdAt);
		ReflectionTestUtils.setField(createdTodo, "updatedAt", updatedAt);
		when(todoService.createTodo(ownerId, title, description)).thenReturn(createdTodo);

		mockMvc.perform(post("/api/v1/todos")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenProvider.createToken(ownerId))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"" + title + "\",\"description\":\"" + description + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", Matchers.endsWith("/api/v1/todos/42")))
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.title").value(title))
				.andExpect(jsonPath("$.description").value(description))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.createdAt").value("2026-10-01T12:30:00Z"))
				.andExpect(jsonPath("$.updatedAt").value("2026-10-02T15:45:00Z"))
				.andExpect(jsonPath("$.user").doesNotExist())
				.andExpect(jsonPath("$.password").doesNotExist());

		verify(todoService).createTodo(eq(ownerId), eq(title), eq(description));
	}
}
