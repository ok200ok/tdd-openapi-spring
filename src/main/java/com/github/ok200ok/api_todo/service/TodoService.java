package com.github.ok200ok.api_todo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.repository.TodoRepository;
import com.github.ok200ok.api_todo.repository.UserRepository;

/**
 * Todoに関する業務処理を担い、Repositoryを組み合わせて所有者とTodoを扱うServiceレイヤーコンポーネント
 * {@code @Service}によりSpring管理Beanとなり、後続のControllerから利用できる
 */
@Service
public class TodoService {

	private final UserRepository userRepository;
	private final TodoRepository todoRepository;

	public TodoService(UserRepository userRepository, TodoRepository todoRepository) {
		this.userRepository = userRepository;
		this.todoRepository = todoRepository;
	}

	/**
	 * 指定された所有者に紐づくTodoを初期状態PENDINGで作成する
	 * Userを先に検索して関連付けることで、必須の所有者を持たないTodoが保存されるのを防ぐ
	 *
	 * @param ownerId Todo所有者のID
	 * @param title Todoのタイトル
	 * @param description Todoの説明。省略時はnull
	 * @return Repositoryが保存したTodo
	 * @throws UserNotFoundException 指定された所有者が存在しない場合
	 */
	public Todo createTodo(UUID ownerId, String title, String description) {
		User owner = userRepository.findById(ownerId)
				.orElseThrow(() -> new UserNotFoundException(ownerId));
		Todo todo = new Todo(owner, title, description, TodoStatusEnum.PENDING);
		return todoRepository.save(todo);
	}

	/**
	 * 指定された所有者に属するTodoをIDで検索する
	 * RepositoryへIDと所有者IDの両方を渡し、他人のTodoを結果として取得しない
	 *
	 * @param todoId 検索するTodoのID
	 * @param ownerId Todo所有者のID
	 * @return 所有者に属するTodo
	 * @throws TodoNotFoundException Todoが存在しないか、指定所有者に属さない場合
	 */
	public Todo findTodoByIdForOwner(Long todoId, UUID ownerId) {
		return todoRepository.findByIdAndUser_Id(todoId, ownerId)
				.orElseThrow(() -> new TodoNotFoundException(todoId, ownerId));
	}

	/**
	 * 指定された所有者のTodo一覧を更新日時降順で取得する
	 * 0件は正常な一覧結果としてRepositoryの空リストをそのまま返す
	 *
	 * @param ownerId Todo所有者のID
	 * @return 所有者のTodo一覧。該当するTodoがない場合は空リスト
	 */
	public List<Todo> findTodosForOwner(UUID ownerId) {
		return todoRepository.findAllByUser_IdOrderByUpdatedAtDescIdDesc(ownerId);
	}

	/**
	 * 指定された所有者のTodoについて、タイトル・説明・状態を一括更新する
	 * @Transactional 検索とEntity変更を同じトランザクション内で行い、JPAのdirty checkingで変更を永続化する。正常終了時にコミットされ、例外終了時は変更がロールバックされる
	 *
	 * @param todoId 更新するTodoのID
	 * @param ownerId Todo所有者のID
	 * @param title 更新後のタイトル
	 * @param description 更新後の説明
	 * @param status 更新後の状態
	 * @return 更新後のTodo
	 * @throws TodoNotFoundException Todoが存在しないか、指定所有者に属さない場合
	 */
	@Transactional
	public Todo updateTodoForOwner(Long todoId, UUID ownerId, String title, String description, TodoStatusEnum status) {
		Todo todo = findTodoByIdForOwner(todoId, ownerId);
		todo.changeDetails(title, description);
		todo.changeStatus(status);
		return todo;
	}

	/**
	 * 指定された所有者のTodoを削除する
	 * 所有者条件付き検索で取得したEntityだけを削除し、未検出時は削除を行わない
	 *
	 * @param todoId 削除するTodoのID
	 * @param ownerId Todo所有者のID
	 * @throws TodoNotFoundException Todoが存在しないか、指定所有者に属さない場合
	 */
	// 検索と削除を一つのDBトランザクションにまとめ、正常終了時に削除をコミットする
	@Transactional
	public void deleteTodoForOwner(Long todoId, UUID ownerId) {
		Todo todo = findTodoByIdForOwner(todoId, ownerId);
		todoRepository.delete(todo);
	}
}
