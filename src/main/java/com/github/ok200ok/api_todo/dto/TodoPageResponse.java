package com.github.ok200ok.api_todo.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.github.ok200ok.api_todo.entity.Todo;

/**
 * Todo一覧APIの出力契約
 * Spring DataのPageや永続化Entityを直接公開せず、公開ページ番号とTodo応答DTOへ変換する
 */
public record TodoPageResponse(
		List<TodoResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	/**
	 * Serviceから受け取ったページ情報をAPIの1始まり表現へ変換する
	 * @param todos 所有者条件を適用済みのTodoページ
	 * @return APIで公開するページ応答
	 */
	public static TodoPageResponse from(Page<Todo> todos) {
		return new TodoPageResponse(
				todos.getContent().stream().map(TodoResponse::from).toList(),
				todos.getNumber() + 1,
				todos.getSize(),
				todos.getTotalElements(),
				todos.getTotalPages());
	}
}
