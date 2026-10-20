package com.github.ok200ok.api_todo.dto;

import java.time.Instant;

import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;

/**
 * Todo APIの出力契約
 * 永続化Entityや所有者情報を外部へ公開せず、APIで合意した項目だけを返す
 *
 * @param id Todoを一意に識別するID
 * @param title Todoのタイトル
 * @param description Todoの詳細説明。未設定の場合はnull
 * @param status Todoの現在の状態
 * @param createdAt Todoの作成日時
 * @param updatedAt Todoの最終更新日時
 */
public record TodoResponse(
		Long id,
		String title,
		String description,
		TodoStatusEnum status,
		Instant createdAt,
		Instant updatedAt) {

	/**
	 * 永続化EntityをAPIの応答DTOへ変換する
	 * @param todo 変換元のTodo Entity
	 * @return 所有者情報を含まないTodo応答
	 */
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
