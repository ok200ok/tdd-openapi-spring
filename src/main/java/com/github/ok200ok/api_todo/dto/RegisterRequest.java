package com.github.ok200ok.api_todo.dto;

import com.github.ok200ok.api_todo.validation.Utf8ByteLength;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * ユーザー登録APIの入力契約
 * HTTP入力をEntityから分離し、Controllerで受け取る項目と検証制約を定義する
 *
 * @param userName ログインに使用する小文字英数字のみのユーザー名
 * @param password 8文字以上かつUTF-8で72バイト以内のパスワード
 * @param displayName 任意の表示名。重複可能で認証には使用しない
 */
public record RegisterRequest(
		@NotBlank(message = "ユーザー名は必須です")
		@Size(max = 50, message = "ユーザー名は50文字以内で入力してください")
		@Pattern(regexp = "^[a-z0-9]+$", message = "ユーザー名は小文字英数字のみ使用できます")
		String userName,

		@NotBlank(message = "パスワードは必須です")
		@Size(min = 8, message = "パスワードは8文字以上で入力してください")
		@Utf8ByteLength(max = 72, message = "パスワードはUTF-8で72バイト以内で入力してください")
		String password,

		@Size(max = 50, message = "表示名は50文字以内で入力してください")
		String displayName) {
}
