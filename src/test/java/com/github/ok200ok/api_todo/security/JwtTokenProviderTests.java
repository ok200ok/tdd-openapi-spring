package com.github.ok200ok.api_todo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * JwtTokenProviderの発行結果をSpring Securityの標準JWT部品で検証する単体テスト
 * SpringコンテキストやDBを起動せず、実際のHMAC署名・claim・有効期限の振る舞いに絞って確認する
 */
class JwtTokenProviderTests {

	private static final SecretKey SIGNING_KEY =
			new SecretKeySpec("test-signing-key-at-least-32-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

	private final JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(SIGNING_KEY));
	private final JwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(SIGNING_KEY)
			.macAlgorithm(MacAlgorithm.HS256)
			.build();

	private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtEncoder);

	/**
	 * 発行tokenを標準デコーダーで検証し、subjectと15分の有効期間を確認する
	 */
	@Test
	void issuesSignedJwtWithUserSubjectAndFifteenMinuteLifetime() {
		UUID userId = UUID.randomUUID();

		String token = jwtTokenProvider.createToken(userId);

		Jwt jwt = jwtDecoder.decode(token);
		assertThat(jwt.getSubject()).isEqualTo(userId.toString());
		assertThat(jwt.getIssuedAt()).isNotNull();
		assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plus(Duration.ofMinutes(15)));
	}

	/**
	 * 別の署名鍵で生成したtokenを標準デコーダーが拒否することを確認する
	 */
	@Test
	void rejectsJwtSignedWithDifferentKey() {
		SecretKey differentKey =
				new SecretKeySpec("different-test-key-at-least-32-bytes".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		JwtEncoder differentEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(differentKey));
		String token = encodeToken(differentEncoder, Instant.now(), Instant.now().plusSeconds(900));

		assertThatThrownBy(() -> jwtDecoder.decode(token))
				.isInstanceOf(JwtException.class);
	}

	/**
	 * 有効期限を過ぎたtokenを標準デコーダーが拒否することを確認する
	 */
	@Test
	void rejectsExpiredJwt() {
		Instant now = Instant.now();
		String token = encodeToken(jwtEncoder, now.minusSeconds(600), now.minusSeconds(300));

		assertThatThrownBy(() -> jwtDecoder.decode(token))
				.isInstanceOf(JwtException.class);
	}

	private String encodeToken(JwtEncoder encoder, Instant issuedAt, Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(UUID.randomUUID().toString())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
