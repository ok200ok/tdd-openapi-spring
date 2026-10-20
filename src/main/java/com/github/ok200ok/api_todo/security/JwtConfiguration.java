package com.github.ok200ok.api_todo.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * JWTの署名・検証に使う標準部品をSpringへ登録する設定
 * Resource ServerはJwtDecoder Beanを要求するため、本番実行でも生成できるようここで提供する
 * 署名鍵は設定から注入し、ソースに秘密値を直接埋め込まない
 */
@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {

	private final String secret;

	/**
	 * @param secret 署名鍵の文字列。app.security.jwt.secret経由で環境変数から上書きできる
	 */
	public JwtConfiguration(@Value("${app.security.jwt.secret}") String secret) {
		this.secret = secret;
	}

	/**
	 * claimを実際のHMAC署名付きJWT文字列へ変換するエンコーダーを提供する
	 * 署名アルゴリズムとtoken文字列の生成はNimbusの標準実装に委譲する
	 */
	@Bean
	JwtEncoder jwtEncoder() {
		return new NimbusJwtEncoder(new ImmutableSecret<>(signingKey()));
	}

	/**
	 * AuthorizationヘッダーのBearer tokenを検証しclaimへ戻すデコーダーを提供する
	 * 署名方式はJwtTokenProviderの発行と揃えるためHS256を明示し、鍵長不足などは起動時に検出させる
	 */
	@Bean
	JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withSecretKey(signingKey())
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
	}

	/**
	 * HS256で使う共有鍵を設定文字列から生成する
	 * HMAC-SHA256は256ビット以上の鍵長を要求するため、設定値は十分長い文字列を使う
	 */
	private SecretKey signingKey() {
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}
}
