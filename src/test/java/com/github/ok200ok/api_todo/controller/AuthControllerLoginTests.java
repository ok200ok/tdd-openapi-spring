package com.github.ok200ok.api_todo.controller;

import static org.hamcrest.Matchers.hasItem;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.ok200ok.api_todo.security.SecurityConfiguration;
import com.github.ok200ok.api_todo.security.TestJwtConfiguration;
import com.github.ok200ok.api_todo.service.AuthService;

/**
 * ログインAPIの成功時HTTP契約、Service連携、資格情報不一致時の401応答、入力検証を検証するWeb層テスト
 * AuthServiceをモックし、SecurityFilterChainを含むHTTP境界だけを確認する
 */
@WebMvcTest(AuthController.class)
@Import({ SecurityConfiguration.class, TestJwtConfiguration.class })
class AuthControllerLoginTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	/**
	 * ログイン成功時に200、tokenのみを含む応答DTOを返し、ユーザー情報を返さないことを確認する
	 */
	@Test
	void logsInAndReturnsToken() throws Exception {
		AuthService.LoginResult result = new AuthService.LoginResult("access-token", 900);
		when(authService.login("alice", "password123")).thenReturn(result);

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"userName\":\"alice\",\"password\":\"password123\"}"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.accessToken").value("access-token"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andExpect(jsonPath("$.userName").doesNotExist())
				.andExpect(jsonPath("$.displayName").doesNotExist())
				.andExpect(jsonPath("$.id").doesNotExist())
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.role").doesNotExist());

		verify(authService).login("alice", "password123");
	}

	/**
	 * 資格情報の不一致を401のProblem Detailsへ変換し、errors拡張と認証チャレンジを付けないことを確認する
	 */
	@Test
	void returnsUnauthorizedWhenCredentialsAreInvalid() throws Exception {
		when(authService.login("alice", "password123"))
				.thenThrow(new BadCredentialsException("Bad credentials"));

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"userName\":\"alice\",\"password\":\"password123\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value("ユーザー名またはパスワードが正しくありません"))
				.andExpect(jsonPath("$.errors").doesNotExist())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));

		verify(authService).login("alice", "password123");
	}

	/**
	 * ログイン入力の各制約違反を400へ変換し、違反した項目名をerrorsへ含めることを確認する
	 * 登録と同じuserName・password制約を適用し、AuthServiceへ一切到達しないことも検証する
	 */
	@ParameterizedTest(name = "{index}: {1}")
	@MethodSource("invalidLoginInputs")
	void rejectsInvalidLoginInput(String body, String expectedField) throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
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
	 * 登録の不正入力ケースからdisplayNameを除き、ログイン入力の制約だけを対象にする
	 */
	static Stream<Arguments> invalidLoginInputs() {
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
				Arguments.of("{\"userName\":\"alice\",\"password\":\"" + seventyThreeBytePassword + "\"}", "password"));
	}
}
