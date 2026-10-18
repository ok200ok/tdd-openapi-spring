package com.github.ok200ok.api_todo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * パスワードを安全に保存するためのPasswordEncoderをSpringへ登録する設定
 * UserServiceへ暗号方式の生成を持ち込まず、登録処理とハッシュ方式を分離する
 */
@Configuration(proxyBeanMethods = false)
public class PasswordEncoderConfiguration {

	/**
	 * BCryptを使うPasswordEncoderをBeanとして提供する
	 * BCryptPasswordEncoderの既定work factorを使い、後から設定に応じて調整できる境界を設ける
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
