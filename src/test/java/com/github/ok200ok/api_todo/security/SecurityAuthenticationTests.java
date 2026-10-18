package com.github.ok200ok.api_todo.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.ok200ok.api_todo.controller.TodoController;
import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.service.TodoService;

/**
 * Resource ServerのBearer認証がTodo APIを保護し、JWT subjectを所有者IDへ結び付けることを検証するMVCテスト
 * {@code @WebMvcTest}でHTTP処理とSecurityFilterChainを起動し、ServiceとJwtDecoderはモックにしてDBを使わない
 */
@WebMvcTest(TodoController.class)
@Import({SecurityConfiguration.class, AuthenticatedUserIdResolver.class})
class SecurityAuthenticationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TodoService todoService;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	/**
	 * Bearer tokenのないTodo API要求をSecurityFilterChainが401で拒否し、業務処理へ進めないことを確認する
	 */
	@Test
	void rejectsTodoRequestWithoutBearerToken() throws Exception {
		mockMvc.perform(get("/api/v1/todos/42"))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(todoService);
	}

	/**
	 * 認証済みJwtのsubjectをUser UUIDとして解決し、TodoServiceの所有者条件へ渡すことを確認する
	 */
	@Test
	void passesJwtSubjectAsOwnerIdToTodoService() throws Exception {
		UUID userId = UUID.randomUUID();
		Instant issuedAt = Instant.now();
		Jwt jwt = Jwt.withTokenValue("valid-token")
				.header("alg", "HS256")
				.subject(userId.toString())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plusSeconds(900))
				.build();
		when(jwtDecoder.decode("valid-token")).thenReturn(jwt);

		User owner = new User("alice", "encoded-password", "Alice", "ROLE_USER");
		Todo todo = new Todo(owner, "Read documentation", null, TodoStatusEnum.PENDING);
		when(todoService.findTodoByIdForOwner(42L, userId)).thenReturn(todo);

		mockMvc.perform(get("/api/v1/todos/42")
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk());

		verify(todoService).findTodoByIdForOwner(42L, userId);
	}

	/**
	 * Resource Serverが不正署名・期限切れとして拒否したtokenでは401を返し、業務処理へ進めないことを確認する
	 */
	@Test
	void rejectsInvalidAndExpiredBearerTokens() throws Exception {
		when(jwtDecoder.decode("invalid-token")).thenThrow(new JwtException("invalid signature"));
		when(jwtDecoder.decode("expired-token")).thenThrow(new JwtException("token expired"));

		mockMvc.perform(get("/api/v1/todos/42")
				.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/todos/42")
				.header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(todoService);
	}

	/**
	 * 認証成功をHTTP sessionへ保存せず、後続要求でもBearer tokenを要求することを確認する
	 */
	@Test
	void doesNotReuseAuthenticationFromHttpSession() throws Exception {
		UUID userId = UUID.randomUUID();
		Instant issuedAt = Instant.now();
		Jwt jwt = Jwt.withTokenValue("valid-token")
				.header("alg", "HS256")
				.subject(userId.toString())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plusSeconds(900))
				.build();
		when(jwtDecoder.decode("valid-token")).thenReturn(jwt);
		User owner = new User("alice", "encoded-password", "Alice", "ROLE_USER");
		Todo todo = new Todo(owner, "Read documentation", null, TodoStatusEnum.PENDING);
		when(todoService.findTodoByIdForOwner(42L, userId)).thenReturn(todo);
		MockHttpSession session = new MockHttpSession();

		mockMvc.perform(get("/api/v1/todos/42")
				.session(session)
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/todos/42").session(session))
				.andExpect(status().isUnauthorized());

		verify(todoService).findTodoByIdForOwner(42L, userId);
	}
}
