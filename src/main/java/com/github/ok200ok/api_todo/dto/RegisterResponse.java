package com.github.ok200ok.api_todo.dto;

/**
 * ユーザー登録APIの出力契約
 * 登録されたユーザーの公開情報とaccess tokenを返し、パスワードやroleは公開しない
 */
public record RegisterResponse(
		String userName,
		String displayName,
		String accessToken,
		String tokenType,
		long expiresIn) {
}
