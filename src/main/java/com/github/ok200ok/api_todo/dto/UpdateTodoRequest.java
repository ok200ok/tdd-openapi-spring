package com.github.ok200ok.api_todo.dto;

import com.github.ok200ok.api_todo.entity.TodoStatusEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Todo更新APIの入力契約
 * PUTで置き換えるタイトル・説明・状態をEntityから分離して受け取り、境界で必須項目を検証する
 */
public record UpdateTodoRequest(
		@NotBlank(message = "タイトルは必須です")
		@Size(max = 100, message = "タイトルは100文字以内で入力してください")
		String title,
		String description,
		@NotNull(message = "状態は必須です")
		TodoStatusEnum status) {

	public UpdateTodoRequest {
		if (title != null) {
			title = title.strip();
		}
	}
}
