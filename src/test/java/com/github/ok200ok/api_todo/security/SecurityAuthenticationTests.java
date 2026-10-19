package com.github.ok200ok.api_todo.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.github.ok200ok.api_todo.controller.TodoController;
import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.service.TodoService;

/**
 * Resource ServerのBearer認証がTodo APIを保護し、JWT subjectを所有者IDへ結び付けることを検証するMVCテスト
 * {@code @WebMvcTest}でHTTP処理とSecurityFilterChainを起動し、TodoServiceだけをモックにしてDBを使わない
 */
@WebMvcTest(TodoController.class)
@Import({
		SecurityConfiguration.class,
		AuthenticatedUserIdResolver.class,
		JwtTokenProvider.class,
		TestJwtConfiguration.class
})
class SecurityAuthenticationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TodoService todoService;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private JwtEncoder jwtEncoder;

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
		String token = jwtTokenProvider.createToken(userId);

		User owner = new User("alice", "encoded-password", "Alice", "ROLE_USER");
		Todo todo = new Todo(owner, "Read documentation", null, TodoStatusEnum.PENDING);
		when(todoService.findTodoByIdForOwner(42L, userId)).thenReturn(todo);

		mockMvc.perform(get("/api/v1/todos/42")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());

		verify(todoService).findTodoByIdForOwner(42L, userId);
	}

	/**
	 * Resource Serverが不正署名・期限切れとして拒否したtokenでは401を返し、業務処理へ進めないことを確認する
	 */
	@Test
	void rejectsInvalidAndExpiredBearerTokens() throws Exception {
		SecretKey differentKey = new SecretKeySpec(
				"another-test-secret-key-that-is-long-enough".getBytes(StandardCharsets.UTF_8),
				"HmacSHA256");
		JwtEncoder differentEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(differentKey));
		String invalidToken = createToken(differentEncoder, UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(900));
		Instant expiredAt = Instant.now().minusSeconds(300);
		String expiredToken = createToken(jwtEncoder, UUID.randomUUID(), expiredAt.minusSeconds(900), expiredAt);

		mockMvc.perform(get("/api/v1/todos/42")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/todos/42")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(todoService);
	}

	/**
	 * 認証成功をHTTP sessionへ保存せず、後続要求でもBearer tokenを要求することを確認する
	 */
	@Test
	void doesNotReuseAuthenticationFromHttpSession() throws Exception {
		UUID userId = UUID.randomUUID();
		String token = jwtTokenProvider.createToken(userId);
		User owner = new User("alice", "encoded-password", "Alice", "ROLE_USER");
		Todo todo = new Todo(owner, "Read documentation", null, TodoStatusEnum.PENDING);
		when(todoService.findTodoByIdForOwner(42L, userId)).thenReturn(todo);
		MockHttpSession session = new MockHttpSession();

		mockMvc.perform(get("/api/v1/todos/42")
				.session(session)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/todos/42").session(session))
				.andExpect(status().isUnauthorized());

		verify(todoService).findTodoByIdForOwner(42L, userId);
	}

	private String createToken(
			JwtEncoder encoder,
			UUID subject,
			Instant issuedAt,
			Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(subject.toString())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
