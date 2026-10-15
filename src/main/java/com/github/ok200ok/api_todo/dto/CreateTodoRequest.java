package com.github.ok200ok.api_todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Todo作成APIの入力契約
 * HTTP入力をEntityから分離し、タイトルの形式をController境界で検証する
 */
public record CreateTodoRequest(
		@NotBlank(message = "タイトルは必須です")
		@Size(max = 100, message = "タイトルは100文字以内で入力してください")
		String title,
		String description) {

	public CreateTodoRequest {
		if (title != null) {
			title = title.strip();
		}
	}
}
