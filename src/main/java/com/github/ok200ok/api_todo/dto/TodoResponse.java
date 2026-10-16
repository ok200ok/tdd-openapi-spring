package com.github.ok200ok.api_todo.dto;

import java.time.Instant;

import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;

/**
 * Todo APIの出力契約
 * 永続化Entityや所有者情報を外部へ公開せず、APIで合意した項目だけを返す
 */
public record TodoResponse(
		Long id,
		String title,
		String description,
		TodoStatusEnum status,
		Instant createdAt,
		Instant updatedAt) {

	public static TodoResponse from(Todo todo) {
		return new TodoResponse(
				todo.getId(),
				todo.getTitle(),
				todo.getDescription(),
				todo.getStatus(),
				todo.getCreatedAt(),
				todo.getUpdatedAt());
	}
}
