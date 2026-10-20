package com.github.ok200ok.api_todo.dto;

/**
 * ログインAPIの出力契約
 * access tokenとその利用方法だけを返し、ユーザー情報や認証内部の値を公開しない
 *
 * @param accessToken 認証済みユーザーに発行されたJWT access token
 * @param tokenType Authorizationヘッダーで指定するtoken種別
 * @param expiresIn access tokenの有効期間（秒）
 */
public record TokenResponse(
		String accessToken,
		String tokenType,
		long expiresIn) {
}
