package com.github.ok200ok.api_todo.dto;

/**
 * ユーザー登録APIの出力契約
 * 登録されたユーザーの公開情報とaccess tokenを返し、パスワードやroleは公開しない
 *
 * @param userName 登録されたユーザー名
 * @param displayName 登録された表示名。未設定の場合はnull
 * @param accessToken 登録直後から利用できるJWT access token
 * @param tokenType Authorizationヘッダーで指定するtoken種別
 * @param expiresIn access tokenの有効期間（秒）
 */
public record RegisterResponse(
		String userName,
		String displayName,
		String accessToken,
		String tokenType,
		long expiresIn) {
}
