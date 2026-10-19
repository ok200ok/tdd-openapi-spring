package com.github.ok200ok.api_todo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Todo APIをBearer JWTで保護するSpring Security設定
 * Resource Serverの標準フィルターにtoken抽出・検証・認証設定を委譲する
 */
@Configuration(proxyBeanMethods = false)
// HttpSecurityをSpringのWeb Security構成へ登録し、SecurityFilterChainを組み立てられるようにする
@EnableWebSecurity
public class SecurityConfiguration {

	/**
	 * Todo APIに認証を要求し、HTTP sessionへ認証状態を保存しないフィルターチェーンを構成する
	 * CSRF保護はCookie等がブラウザーから自動送信される認証を使わないため無効化し、認証をBearer tokenに限定する
	 * STATELESSはSecurityContextをHTTP sessionへ保存せず、要求ごとにtokenを検証する
	 * 認証APIと登録前のSwagger UIなどは公開し、Todo APIだけを認証必須とする
	 * 認証APIを明示的にpermitAllにすることで、将来anyRequestをdenyAllへ変更しても登録・ログインを維持する
	 * oauth2ResourceServer.jwtはBearer tokenの抽出とJwtDecoderによる検証をSpring Security標準フィルターへ委譲する
	 */
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/v1/auth/**").permitAll()
						.requestMatchers("/api/v1/todos/**").authenticated()
						.anyRequest().permitAll())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return http.build();
	}
}
