package com.github.ok200ok.api_todo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.github.ok200ok.api_todo.security.JpaUserDetailsService;

/**
 * ログイン時の資格情報検証に使うAuthenticationManagerを構成する設定
 * AuthServiceは認証手順をAuthenticationManagerへ委譲し、この設定がDBのUserとPasswordEncoderを接続する
 */
@Configuration(proxyBeanMethods = false)
public class AuthenticationManagerConfiguration {

	/**
	 * UserDetailsServiceでDBのUserを取得し、PasswordEncoderで平文パスワードと保存済みハッシュを照合する
	 * ユーザー不存在とパスワード不一致を区別せずBadCredentialsExceptionへ統一し、アカウント存在の推測を防ぐ
	 * ProviderManagerは複数の認証Providerをまとめる標準実装で、現時点ではDB認証のProviderだけを登録する
	 */
	@Bean
	AuthenticationManager authenticationManager(
			JpaUserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		provider.setHideUserNotFoundExceptions(true);
		return new ProviderManager(provider);
	}
}
