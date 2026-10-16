package com.github.ok200ok.api_todo.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.github.ok200ok.api_todo.repository.UserRepository;

/**
 * localプロファイルで設定ユーザー名からTodo所有者のDB IDを解決する
 * Controllerを認証方式から分離し、後続の認証導入時に境界実装を差し替えられる
 */
@Component
@Profile("local")
public class LocalOwnerIdResolver implements OwnerIdResolver {

	private final UserRepository userRepository;
	private final String ownerUsername;

	public LocalOwnerIdResolver(
			UserRepository userRepository,
			@Value("${app.local-owner.username}") String ownerUsername) {
		this.userRepository = userRepository;
		this.ownerUsername = ownerUsername;
	}

	/**
	 * 設定されたユーザー名に対応するUserのDB上のUUIDを返す
	 * @return リクエストを処理するローカルデモユーザーのID
	 * @throws IllegalStateException 設定ユーザーが初期化されていない場合
	 */
	@Override
	public UUID resolveOwnerId() {
		return userRepository.findByUserName(ownerUsername)
				.orElseThrow(() -> new IllegalStateException(
						"ローカル所有者が初期化されていません: " + ownerUsername))
				.getId();
	}
}
