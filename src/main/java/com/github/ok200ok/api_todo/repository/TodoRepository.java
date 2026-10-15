package com.github.ok200ok.api_todo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	/**
	 * 指定したUserに属するTodoを更新日時の降順、同時刻ではIDの降順で取得する
	 * User_Idで所有者条件をDB検索に含め、OrderByで一覧の安定した表示順を定義する
	 */
	List<Todo> findAllByUser_IdOrderByUpdatedAtDescIdDesc(UUID userId);

	/**
	 * 指定したUserに属するTodoを更新日時降順、同時刻ではID降順でDBページ分割して取得する
	 * {@code Pageable}がページ位置と件数をSpring Data JPAへ渡し、全件をアプリケーションへ読み込まずに取得する
	 * {@code Page}は現在ページの内容に加えて総件数・総ページ数を提供するため、必要に応じて件数確認クエリも実行される
	 */
	Page<Todo> findAllByUser_IdOrderByUpdatedAtDescIdDesc(UUID userId, Pageable pageable);
}
