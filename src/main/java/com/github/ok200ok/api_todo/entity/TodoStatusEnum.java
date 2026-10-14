package com.github.ok200ok.api_todo.entity;

/**
 * Todoの進行状態を表す値の集合
 * {@code Todo.status}では{@code EnumType.STRING}により、各定数名がDBに保存される。定数名を変更する場合は既存DBデータとの互換性に注意する
 */
public enum TodoStatusEnum {
	PENDING,
	IN_PROGRESS,
	COMPLETED
}
