package com.github.ok200ok.api_todo.dto;

/**
 * ログインAPIの出力契約
 * access tokenとその利用方法だけを返し、ユーザー情報や認証内部の値を公開しない
 */
public record TokenResponse(
		String accessToken,
		String tokenType,
		long expiresIn) {
}
