package com.github.ok200ok.api_todo.service;

import java.util.UUID;

/**
 * Todoが存在しないか、指定された所有者に属さない場合にServiceが通知する例外
 * 両方を同じ結果として扱い、呼び出し元に他ユーザーのTodoの存在を明かさない
 */
public class TodoNotFoundException extends RuntimeException {

	public TodoNotFoundException(Long todoId, UUID ownerId) {
		super("Todo not found for owner: todoId=" + todoId + ", ownerId=" + ownerId);
	}
}
