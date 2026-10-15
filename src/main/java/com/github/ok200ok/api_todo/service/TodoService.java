package com.github.ok200ok.api_todo.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

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
}
