package com.github.ok200ok.api_todo.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

	/**
	 * 登録入力の各制約違反を400へ変換し、違反した項目名をerrorsへ含めることを確認する
	 * 同一メソッドを複数入力で実行し、AuthServiceへ一切到達しないことも検証する
	 */
	@ParameterizedTest(name = "{index}: {1}")
	@MethodSource("invalidRegistrationInputs")
	void rejectsInvalidRegistrationInput(String body, String expectedField) throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("入力内容を確認してください"))
				.andExpect(jsonPath("$.errors[*].field", hasItem(expectedField)));

		verifyNoInteractions(authService);
	}

	/**
	 * 入力検証のパラメーターをJSON本文と違反項目名の組で提供する
	 */
	static Stream<Arguments> invalidRegistrationInputs() {
		String validPassword = "password123";
		String seventyThreeBytePassword = "あ".repeat(24) + "a";

		return Stream.of(
				Arguments.of("{\"password\":\"" + validPassword + "\"}", "userName"),
				Arguments.of("{\"userName\":\"   \",\"password\":\"" + validPassword + "\"}", "userName"),
				Arguments.of("{\"userName\":\"Alice\",\"password\":\"" + validPassword + "\"}", "userName"),
				Arguments.of("{\"userName\":\"alice!\",\"password\":\"" + validPassword + "\"}", "userName"),
				Arguments.of("{\"userName\":\"" + "a".repeat(51) + "\",\"password\":\"" + validPassword + "\"}", "userName"),
				Arguments.of("{\"userName\":\"alice\"}", "password"),
				Arguments.of("{\"userName\":\"alice\",\"password\":\"1234567\"}", "password"),
				Arguments.of("{\"userName\":\"alice\",\"password\":\"" + seventyThreeBytePassword + "\"}", "password"),
				Arguments.of("{\"userName\":\"alice\",\"password\":\"" + validPassword
						+ "\",\"displayName\":\"" + "a".repeat(51) + "\"}", "displayName"));
	}

	/**
	 * UTF-8で72バイトちょうどのパスワードを受理し、Serviceへ同じ文字列を渡すことを確認する
	 */
	@Test
	void acceptsPasswordAtUtf8ByteLimit() throws Exception {
		String password = "あ".repeat(24);
		User user = new User("alice", "encoded-password", "Alice", "ROLE_USER");
		AuthService.RegistrationResult result =
				new AuthService.RegistrationResult(user, "access-token", 900);
		when(authService.register("alice", password, "Alice")).thenReturn(result);

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"userName\":\"alice\",\"password\":\"" + password + "\",\"displayName\":\"Alice\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken").value("access-token"));

		verify(authService).register("alice", password, "Alice");
	}

	/**
	 * 未知フィールドとJSON構文エラーを400のProblem Detailsで拒否し、errorsを付けないことを確認する
	 */
	@Test
	void rejectsUnknownFieldsAndMalformedJsonOnRegistration() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"userName\":\"alice\",\"password\":\"password123\",\"unknown\":\"x\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("リクエスト本文を読み取れません"))
				.andExpect(jsonPath("$.errors").doesNotExist());

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"userName\":\"alice\",\"password\":"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("リクエスト本文を読み取れません"))
				.andExpect(jsonPath("$.errors").doesNotExist());

		verifyNoInteractions(authService);
	}
}
