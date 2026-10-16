package com.github.ok200ok.api_todo.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.repository.UserRepository;
import com.github.ok200ok.api_todo.service.TodoService;

/**
 * 実際のローカル所有者ResolverからControllerを通り、ServiceへUUIDが渡る境界を検証する
 * Web MVCだけを起動し、ユーザー検索と業務処理はRepository・Serviceモックに置き換える
 */
@WebMvcTest
@ActiveProfiles("local")
@TestPropertySource(properties = "app.local-owner.username=localdemo")
@Import(LocalOwnerIdResolver.class)
class LocalOwnerIdResolverControllerTests {

	private static final String OWNER_USERNAME = "localdemo";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TodoService todoService;

	@MockitoBean
	private UserRepository userRepository;

	/**
	 * 設定ユーザー名で取得したUserのDB UUIDをResolverが返し、ControllerがServiceへ渡す
	 */
	@Test
	void passesResolvedOwnerIdToService() throws Exception {
		UUID ownerId = UUID.randomUUID();
		User owner = new User(OWNER_USERNAME, "local-only-not-a-login-password", "Local demo", "ROLE_USER");
		ReflectionTestUtils.setField(owner, "id", ownerId);
		ReflectionTestUtils.setField(owner, "createdAt", Instant.parse("2026-10-01T12:30:00Z"));
		ReflectionTestUtils.setField(owner, "updatedAt", Instant.parse("2026-10-01T12:30:00Z"));
		Todo todo = new Todo(owner, "Read sample", null, TodoStatusEnum.PENDING);
		ReflectionTestUtils.setField(todo, "id", 42L);
		ReflectionTestUtils.setField(todo, "createdAt", Instant.parse("2026-10-01T12:30:00Z"));
		ReflectionTestUtils.setField(todo, "updatedAt", Instant.parse("2026-10-01T12:30:00Z"));
		when(userRepository.findByUserName(OWNER_USERNAME)).thenReturn(Optional.of(owner));
		when(todoService.findTodoByIdForOwner(42L, ownerId)).thenReturn(todo);

		mockMvc.perform(get("/api/v1/todos/42"))
				.andExpect(status().isOk());

		verify(userRepository).findByUserName(OWNER_USERNAME);
		verify(todoService).findTodoByIdForOwner(42L, ownerId);
	}
}
