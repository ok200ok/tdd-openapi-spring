package com.github.ok200ok.api_todo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.repository.UserRepository;

/**
 * ローカル所有者ID解決境界が設定ユーザー名からDB上のUUIDを取得することを検証する単体テスト
 * UserRepositoryをモックし、設定値を使った検索と返却IDを確認する
 */
@ExtendWith(MockitoExtension.class)
class LocalOwnerIdResolverTests {

	private static final String OWNER_USERNAME = "localdemo";

	@Mock
	private UserRepository userRepository;

	/**
	 * 設定されたユーザー名で検索し、見つかったUserのUUIDを返す
	 */
	@Test
	void resolvesDatabaseOwnerIdByConfiguredUsername() {
		UUID ownerId = UUID.randomUUID();
		User owner = new User(OWNER_USERNAME, "local-only-not-a-login-password", "Local demo", "ROLE_USER");
		ReflectionTestUtils.setField(owner, "id", ownerId);
		ReflectionTestUtils.setField(owner, "createdAt", Instant.parse("2026-10-01T12:30:00Z"));
		ReflectionTestUtils.setField(owner, "updatedAt", Instant.parse("2026-10-01T12:30:00Z"));
		when(userRepository.findByUserName(OWNER_USERNAME)).thenReturn(Optional.of(owner));

		LocalOwnerIdResolver resolver = new LocalOwnerIdResolver(userRepository, OWNER_USERNAME);

		assertThat(resolver.resolveOwnerId()).isEqualTo(ownerId);
		verify(userRepository).findByUserName(OWNER_USERNAME);
	}
}
