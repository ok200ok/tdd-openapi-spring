package com.github.ok200ok.api_todo.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.repository.UserRepository;
import com.github.ok200ok.api_todo.security.JwtTokenProvider;

/**
 * ユーザー登録・ログインのユースケースを調整し、JWT access tokenの発行をJwtTokenProviderへ委譲するService
 * 登録ではUserServiceの重複確認・ハッシュ化・保存を再利用し、ログインではAuthenticationManagerへ資格情報の検証を委譲する
 * どちらも保存済みUserのUUIDをtokenのsubjectに使い、認証方式の詳細をControllerへ持ち込まない
 */
// 認証ユースケースをSpringのBeanとして登録し、Controllerから注入できるようにする
@Service
public class AuthService {

	private final UserService userService;
	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthService(
			UserService userService,
			AuthenticationManager authenticationManager,
			UserRepository userRepository,
			JwtTokenProvider jwtTokenProvider) {
		this.userService = userService;
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
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
	 * 入力された資格情報をAuthenticationManagerで検証し、認証済みUserのUUIDでaccess tokenを発行する
	 *
	 * @param userName ログインに使用するユーザー名
	 * @param rawPassword 照合する平文パスワード
	 * @return access tokenと有効期間（秒）
	 */
	public LoginResult login(String userName, String rawPassword) {
		// パスワード照合・ユーザー不在の隠蔽などの認証手順はAuthenticationManagerの標準実装へ委ねる
		Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(userName, rawPassword));

		// 認証に成功した主体の名前でUserを再取得し、JWT subjectに使うDB上のUUIDを得る
		User user = userRepository.findByUserName(authentication.getName())
				.orElseThrow(() -> new IllegalStateException(
						"認証済みユーザーが見つかりません: " + authentication.getName()));

		String accessToken = jwtTokenProvider.createToken(user.getId());
		return new LoginResult(accessToken, jwtTokenProvider.getExpiresInSeconds());
	}

	/**
	 * 登録結果としてControllerへ返すUserとtokenの組
	 */
	public record RegistrationResult(User user, String accessToken, long expiresInSeconds) {
	}

	/**
	 * ログイン結果としてControllerへ返すaccess tokenと有効期間
	 */
	public record LoginResult(String accessToken, long expiresInSeconds) {
	}
}
