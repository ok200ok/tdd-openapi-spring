package com.github.ok200ok.api_todo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.repository.UserRepository;

/**
 * JpaUserDetailsServiceがUserRepositoryのUserをSpring SecurityのUserDetailsへ変換することを検証する単体テスト
 * Repositoryをモックし、SpringコンテキストやDBを起動せず、ユーザー名・パスワード・権限のマッピングに絞る
 */
@ExtendWith(MockitoExtension.class)
class JpaUserDetailsServiceTests {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private JpaUserDetailsService userDetailsService;

	/**
	 * 登録済みUserのユーザー名・保存済みパスワード・roleをUserDetailsへ変換することを確認する
	 */
	@Test
	void loadsUserDetailsWithStoredPasswordAndRole() {
		User user = new User("alice", "encoded-password", "Alice", "ROLE_USER");
		when(userRepository.findByUserName("alice")).thenReturn(Optional.of(user));

		UserDetails result = userDetailsService.loadUserByUsername("alice");

		assertThat(result.getUsername()).isEqualTo("alice");
		assertThat(result.getPassword()).isEqualTo("encoded-password");
		assertThat(result.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList())
				.containsExactly("ROLE_USER");
		verify(userRepository).findByUserName("alice");
	}

	/**
	 * RepositoryがUserを見つけられない場合にUsernameNotFoundExceptionを送出することを確認する
	 */
	@Test
	void throwsUsernameNotFoundExceptionForUnknownUserName() {
		when(userRepository.findByUserName("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing"))
				.isInstanceOf(UsernameNotFoundException.class);

		verify(userRepository).findByUserName("missing");
	}
}
