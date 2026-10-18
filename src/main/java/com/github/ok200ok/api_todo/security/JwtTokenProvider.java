package com.github.ok200ok.api_todo.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

/**
 * Spring Securityの標準JwtEncoderを使ってUser向けaccess tokenを発行する
 * JWTの署名やシリアライズは標準実装に委譲し、このクラスはアプリ固有claimと有効期間を定義する
 */
@Component
public class JwtTokenProvider {

	private static final long ACCESS_TOKEN_LIFETIME_MINUTES = 15;

	private final JwtEncoder jwtEncoder;

	public JwtTokenProvider(JwtEncoder jwtEncoder) {
		this.jwtEncoder = jwtEncoder;
	}

	/**
	 * User UUIDをsubjectにした15分有効のHS256署名JWTを発行する
	 *
	 * @param userId 認証対象UserのUUID
	 * @return AuthorizationヘッダーのBearer tokenとして使えるJWT文字列
	 */
	public String createToken(UUID userId) {
		Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(userId.toString())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(ACCESS_TOKEN_LIFETIME_MINUTES, ChronoUnit.MINUTES))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
