package com.github.ok200ok.api_todo.dto;

import com.github.ok200ok.api_todo.validation.Utf8ByteLength;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * ログインAPIの入力契約
 * 登録と同じユーザー名・パスワード制約を適用し、表示名など認証に不要な項目は受け取らない
 *
 * @param userName ログインに使用するユーザー名
 * @param password 照合するパスワード
 */
public record LoginRequest(
		@NotBlank(message = "ユーザー名は必須です")
		@Size(max = 50, message = "ユーザー名は50文字以内で入力してください")
		@Pattern(regexp = "^[a-z0-9]+$", message = "ユーザー名は小文字英数字のみ使用できます")
		String userName,

		@NotBlank(message = "パスワードは必須です")
		@Size(min = 8, message = "パスワードは8文字以上で入力してください")
		@Utf8ByteLength(max = 72, message = "パスワードはUTF-8で72バイト以内で入力してください")
		String password) {
}
