package com.github.ok200ok.api_todo.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Web層テストでBearer tokenを発行・検証するための共通JWT設定
 * テスト専用の固定鍵を使い、アプリ設定やDBに依存せず認証済み要求を再現する
 * {@code @TestConfiguration}は通常のコンポーネントスキャン対象外で、テストクラスから明示的にImportしたときだけ有効になる
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestJwtConfiguration {

	static final SecretKey TEST_KEY = new SecretKeySpec(
			"test-secret-key-for-security-mvc-tests-long".getBytes(StandardCharsets.UTF_8),
			"HmacSHA256");

	@Bean
	JwtEncoder jwtEncoder() {
		return new NimbusJwtEncoder(new ImmutableSecret<>(TEST_KEY));
	}

	@Bean
	JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withSecretKey(TEST_KEY)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
	}
}
