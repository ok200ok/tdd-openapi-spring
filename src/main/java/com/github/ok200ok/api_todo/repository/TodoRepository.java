package com.github.ok200ok.api_todo.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.ok200ok.api_todo.entity.Todo;

/**
 * Todoの永続化と検索を担うRepositoryレイヤーコンポーネント
 * {@code JpaRepository<Todo, Long>}から基本的なCRUD操作が提供され、所有者条件付き検索は派生クエリとして宣言する
 */
public interface TodoRepository extends JpaRepository<Todo, Long> {

	/**
	 * Todo IDと関連するUserのIDが一致するTodoを検索する
	 * {@code User_Id}のアンダースコアは関連プロパティuserからidへたどる境界を明示し、他ユーザーのTodoを検索結果に含めない
	 * {@code Optional.empty()}はTodo不存在と所有者不一致の両方を表し、呼び出し元へTodoの存在を漏らさない
	 */
	Optional<Todo> findByIdAndUser_Id(Long id, UUID userId);
}
