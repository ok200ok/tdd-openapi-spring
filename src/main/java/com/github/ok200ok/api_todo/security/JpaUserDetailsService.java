package com.github.ok200ok.api_todo.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.repository.UserRepository;

/**
 * UserRepositoryからUserを取得し、Spring SecurityのUserDetailsへ変換するUserDetailsService実装
 * 認証時にDaoAuthenticationProviderから呼び出され、DBに保存されたパスワードハッシュとroleをSecurityの認証契約へ橋渡しする
 */
// UserDetailsService実装をSpringのBeanとして登録し、認証設定から注入できるようにする
@Service
public class JpaUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public JpaUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * ユーザー名に一致するUserをDBから取得し、Spring SecurityのUserDetailsへ変換する
	 * パスワードは平文へ戻さず、登録時にハッシュ化された値をそのまま渡してPasswordEncoderの照合に使わせる
	 *
	 * @param username ログインで指定されたユーザー名
	 * @return 認証に必要なユーザー名・パスワードハッシュ・権限を持つUserDetails
	 * @throws UsernameNotFoundException ユーザー名に一致するUserが存在しない場合
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUserName(username)
				.orElseThrow(() -> new UsernameNotFoundException("ユーザーが見つかりません: " + username));

		// ドメインのUser EntityとSpring SecurityのUserクラスは同名のため、Security側は完全修飾名で参照する
		return org.springframework.security.core.userdetails.User.withUsername(user.getUserName())
				.password(user.getPassword())
				.authorities(user.getRole())
				.build();
	}
}
