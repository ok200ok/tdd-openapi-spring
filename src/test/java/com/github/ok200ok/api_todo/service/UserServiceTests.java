package com.github.ok200ok.api_todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.repository.UserRepository;

/**
 * UserServiceの登録処理を、RepositoryとPasswordEncoderをモックにして検証する単体テスト
 * SpringコンテキストやDBを起動せず、重複確認・パスワードのハッシュ化・保存内容の組み立てに絞って確認する
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTests {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UserService userService;

	/**
	 * 重複しないユーザーを、ハッシュ化したパスワードと一般ユーザー権限で保存することを確認する
	 */
	@Test
	void registersUserWithEncodedPasswordAndUserRole() {
		when(userRepository.findByUserName("alice")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = userService.registerUser("alice", "plain-password", "Alice");

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User savedUser = userCaptor.getValue();
		assertThat(savedUser.getUserName()).isEqualTo("alice");
		assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
		assertThat(savedUser.getPassword()).isNotEqualTo("plain-password");
		assertThat(savedUser.getDisplayName()).isEqualTo("Alice");
		assertThat(savedUser.getRole()).isEqualTo("ROLE_USER");
		assertThat(result).isSameAs(savedUser);

		verify(userRepository).findByUserName("alice");
		verify(passwordEncoder).encode("plain-password");
	}

	/**
	 * 登録済みユーザー名を拒否し、不要なハッシュ化や保存を行わないことを確認する
	 */
	@Test
	void throwsUserAlreadyExistsExceptionWithoutEncodingOrSaving() {
		User existingUser = new User("alice", "existing-hash", null, "ROLE_USER");
		when(userRepository.findByUserName("alice")).thenReturn(Optional.of(existingUser));

		assertThatThrownBy(() -> userService.registerUser("alice", "plain-password", "Alice"))
				.isInstanceOf(UserAlreadyExistsException.class);

		verify(passwordEncoder, never()).encode(any());
		verify(userRepository, never()).save(any(User.class));
	}

	/**
	 * 任意項目の表示名を省略しても、パスワードをハッシュ化して登録できることを確認する
	 */
	@Test
	void registersUserWithoutDisplayName() {
		when(userRepository.findByUserName("alice")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = userService.registerUser("alice", "plain-password", null);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		assertThat(userCaptor.getValue().getDisplayName()).isNull();
		assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
		assertThat(result).isSameAs(userCaptor.getValue());
	}
}
