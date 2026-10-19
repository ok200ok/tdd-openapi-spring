package com.github.ok200ok.api_todo.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.security.SecurityConfiguration;
import com.github.ok200ok.api_todo.security.TestJwtConfiguration;
import com.github.ok200ok.api_todo.service.AuthService;
import com.github.ok200ok.api_todo.service.UserAlreadyExistsException;

/**
 * ユーザー登録APIの成功時HTTP契約、Service連携、重複時の409応答を検証するWeb層テスト
 * AuthServiceをモックし、SecurityFilterChainを含むHTTP境界だけを確認する
 */
@WebMvcTest(AuthController.class)
@Import({ SecurityConfiguration.class, TestJwtConfiguration.class })
class AuthControllerRegisterTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	/**
	 * 登録成功時に201、tokenを含む応答DTOを返し、Locationや機密情報を返さないことを確認する
	 */
	@Test
	void registersUserAndReturnsCreatedWithToken() throws Exception {
		User user = new User("alice", "encoded-password", "Alice", "ROLE_USER");
		AuthService.RegistrationResult result =
				new AuthService.RegistrationResult(user, "access-token", 900);
		when(authService.register("alice", "password123", "Alice")).thenReturn(result);

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"userName\":\"alice\",\"password\":\"password123\",\"displayName\":\"Alice\"}"))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.userName").value("alice"))
				.andExpect(jsonPath("$.displayName").value("Alice"))
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andExpect(jsonPath("$.id").doesNotExist())
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.role").doesNotExist())
				.andExpect(header().doesNotExist(HttpHeaders.LOCATION));

		verify(authService).register("alice", "password123", "Alice");
	}

	/**
	 * 表示名を省略した登録を受理し、Serviceへnullを渡すことを確認する
	 */
	@Test
	void registersUserWithoutDisplayName() throws Exception {
		User user = new User("alice", "encoded-password", null, "ROLE_USER");
		AuthService.RegistrationResult result =
				new AuthService.RegistrationResult(user, "access-token", 900);
		when(authService.register("alice", "password123", null)).thenReturn(result);

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"userName\":\"alice\",\"password\":\"password123\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.displayName").value(nullValue()));

		verify(authService).register("alice", "password123", null);
	}

	/**
	 * 登録済みユーザー名を409のProblem Detailsへ変換し、errors拡張を付けないことを確認する
	 */
	@Test
	void returnsConflictWhenUserNameAlreadyExists() throws Exception {
		when(authService.register("alice", "password123", "Alice"))
				.thenThrow(new UserAlreadyExistsException("alice"));

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"userName\":\"alice\",\"password\":\"password123\",\"displayName\":\"Alice\"}"))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.detail").value("指定されたユーザー名は既に登録されています"))
				.andExpect(jsonPath("$.errors").doesNotExist());

		verify(authService).register("alice", "password123", "Alice");
	}
}
