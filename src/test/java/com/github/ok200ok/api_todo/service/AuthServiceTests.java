package com.github.ok200ok.api_todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.repository.UserRepository;
import com.github.ok200ok.api_todo.security.JwtTokenProvider;

/**
 * AuthServiceの登録・ログインユースケースを、依存コンポーネントをモックにして検証する単体テスト
 * SpringコンテキストやDBを起動せず、登録委譲・認証委譲・token発行・例外の伝播に絞る
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

	@Mock
	private UserService userService;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private UserRepository userRepository;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@InjectMocks
	private AuthService authService;

	/**
	 * 新規ユーザーを登録し、返されたUserのUUIDでtokenを発行することを確認する
	 */
	@Test
	void registersUserAndIssuesTokenForNewUser() {
		UUID userId = UUID.randomUUID();
		User user = new User("alice", "encoded-password", "Alice", "ROLE_USER");
		ReflectionTestUtils.setField(user, "id", userId);
		when(userService.registerUser("alice", "plain-password", "Alice")).thenReturn(user);
		when(jwtTokenProvider.createToken(userId)).thenReturn("access-token");
		when(jwtTokenProvider.getExpiresInSeconds()).thenReturn(900L);

		AuthService.RegistrationResult result = authService.register("alice", "plain-password", "Alice");

		assertThat(result.user()).isSameAs(user);
		assertThat(result.accessToken()).isEqualTo("access-token");
		assertThat(result.expiresInSeconds()).isEqualTo(900);
		verify(userService).registerUser("alice", "plain-password", "Alice");
		verify(jwtTokenProvider).createToken(userId);
		verify(jwtTokenProvider).getExpiresInSeconds();
	}

	/**
	 * 登録済みユーザー名の例外をそのまま伝播し、tokenを発行しないことを確認する
	 */
	@Test
	void propagatesDuplicateUserNameWithoutIssuingToken() {
		when(userService.registerUser("alice", "plain-password", "Alice"))
				.thenThrow(new UserAlreadyExistsException("alice"));

		assertThatThrownBy(() -> authService.register("alice", "plain-password", "Alice"))
				.isInstanceOf(UserAlreadyExistsException.class);

		verify(userService).registerUser("alice", "plain-password", "Alice");
		verifyNoInteractions(jwtTokenProvider);
	}

	/**
	 * 入力された資格情報をAuthenticationManagerで検証し、認証済みユーザーのUUIDでtokenを発行することを確認する
	 */
	@Test
	void authenticatesCredentialsAndIssuesTokenForUserUuid() {
		UUID userId = UUID.randomUUID();
		User user = new User("alice", "encoded-password", "Alice", "ROLE_USER");
		ReflectionTestUtils.setField(user, "id", userId);
		Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
				"alice", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
		when(authenticationManager.authenticate(any())).thenReturn(authentication);
		when(userRepository.findByUserName("alice")).thenReturn(Optional.of(user));
		when(jwtTokenProvider.createToken(userId)).thenReturn("access-token");
		when(jwtTokenProvider.getExpiresInSeconds()).thenReturn(900L);

		AuthService.LoginResult result = authService.login("alice", "plain-password");

		assertThat(result.accessToken()).isEqualTo("access-token");
		assertThat(result.expiresInSeconds()).isEqualTo(900);

		ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
		verify(authenticationManager).authenticate(authenticationCaptor.capture());
		assertThat(authenticationCaptor.getValue().getPrincipal()).isEqualTo("alice");
		assertThat(authenticationCaptor.getValue().getCredentials()).isEqualTo("plain-password");
		verify(userRepository).findByUserName("alice");
		verify(jwtTokenProvider).createToken(userId);
		verify(jwtTokenProvider).getExpiresInSeconds();
	}

	/**
	 * 認証失敗の例外をそのまま伝播し、User再検索とtoken発行を行わないことを確認する
	 */
	@Test
	void propagatesAuthenticationFailureWithoutIssuingToken() {
		when(authenticationManager.authenticate(any()))
				.thenThrow(new BadCredentialsException("Bad credentials"));

		assertThatThrownBy(() -> authService.login("alice", "plain-password"))
				.isInstanceOf(BadCredentialsException.class);

		verifyNoInteractions(userRepository, jwtTokenProvider);
	}
}
