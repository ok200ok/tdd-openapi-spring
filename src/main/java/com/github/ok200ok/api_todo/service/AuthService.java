package com.github.ok200ok.api_todo.service;

import org.springframework.stereotype.Service;

import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.security.JwtTokenProvider;

/**
 * ユーザー登録・ログインのユースケースを調整し、JWT access tokenの発行をJwtTokenProviderへ委譲するService
 * 登録ではUserServiceの重複確認・ハッシュ化・保存を再利用し、保存済みUserのUUIDからtokenを発行する
 */
// 認証ユースケースをSpringのBeanとして登録し、Controllerから注入できるようにする
@Service
public class AuthService {

	private final UserService userService;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthService(UserService userService, JwtTokenProvider jwtTokenProvider) {
		this.userService = userService;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	/**
	 * 新規ユーザーを登録し、登録直後に利用できるaccess tokenを発行する
	 *
	 * @param userName ログインに使用するユーザー名
	 * @param rawPassword ハッシュ化前のパスワード
	 * @param displayName 任意の表示名
	 * @return 登録されたUserとaccess token、有効期間（秒）
	 */
	public RegistrationResult register(String userName, String rawPassword, String displayName) {
		// 重複確認・パスワードハッシュ化・保存はUserServiceの責務に委ねる
		User user = userService.registerUser(userName, rawPassword, displayName);
		// tokenのsubjectにはDBで一意なUser UUIDを使い、ユーザー名変更の影響を受けないようにする
		String accessToken = jwtTokenProvider.createToken(user.getId());
		return new RegistrationResult(user, accessToken, jwtTokenProvider.getExpiresInSeconds());
	}

	/**
	 * 登録結果としてControllerへ返すUserとtokenの組
	 */
	public record RegistrationResult(User user, String accessToken, long expiresInSeconds) {
	}
}
