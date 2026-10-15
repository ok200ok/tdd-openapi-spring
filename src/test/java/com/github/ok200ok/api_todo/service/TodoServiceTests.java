package com.github.ok200ok.api_todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.repository.TodoRepository;
import com.github.ok200ok.api_todo.repository.UserRepository;

/**
 * RepositoryをMockitoのモックに置き換え、TodoServiceの作成処理だけを検証する単体テスト
 * {@code @ExtendWith(MockitoExtension.class)}は各テストのMockitoモックを初期化し、SpringコンテキストやDBを起動せずに実行する
 */
@ExtendWith(MockitoExtension.class)
class TodoServiceTests {

	@Mock
	private UserRepository userRepository;

	@Mock
	private TodoRepository todoRepository;

	/**
	 * {@code @InjectMocks}はテスト対象を作成し、{@code @Mock}のRepositoryを注入する
	 * SpringのDIではなくMockitoによる注入なので、Service単体の振る舞いを依存先から切り離して確認できる
	 */
	@InjectMocks
	private TodoService todoService;

	/**
	 * UserRepositoryが指定IDのUserを返す場合、作成内容と初期状態が保存され、Serviceが保存結果を返すことを確認する
	 * {@code when(...).thenReturn(...)}でUser検索のモック応答を決め、DBを使わずServiceへ既知の所有者を渡す
	 * {@code thenAnswer}はTodoRepositoryのsaveへ渡された同じTodoを返すようにし、実DBでの採番や永続化ではなくServiceから戻る値を確認する
	 * {@code ArgumentCaptor}でsaveに渡されたEntityを捕捉し、所有者・タイトル・説明・PENDING状態が正しく組み立てられたか検証する
	 * {@code verify}はRepositoryへの保存呼び出しを確認し、最後の同一性検証はServiceが保存結果をそのまま返すことを確認する
	 */
	@Test
	void createsTodoForOwnerWithPendingStatus() {
		UUID ownerId = UUID.randomUUID();
		User owner = new User("alice", "hashed-password", null, "ROLE_USER");
		when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
		when(todoRepository.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Todo result = todoService.createTodo(ownerId, "Write tests", "Cover the service behavior");

		ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
		verify(todoRepository).save(todoCaptor.capture());
		Todo savedTodo = todoCaptor.getValue();
		assertThat(savedTodo.getUser()).isSameAs(owner);
		assertThat(savedTodo.getTitle()).isEqualTo("Write tests");
		assertThat(savedTodo.getDescription()).isEqualTo("Cover the service behavior");
		assertThat(savedTodo.getStatus()).isEqualTo(TodoStatusEnum.PENDING);
		assertThat(result).isSameAs(savedTodo);
	}

	/**
	 * UserRepositoryが指定IDのUserを見つけられない場合、所有者不在の例外となりTodoを保存しないことを確認する
	 * {@code Optional.empty()}を返すよう検索モックを設定し、UserRepositoryの未検出結果をServiceへ渡す
	 * {@code assertThatThrownBy}でService呼び出しが{@code UserNotFoundException}を送出する契約を検証する
	 * {@code verify(..., never())}はTodoRepositoryへのsaveが一度も呼び出されないことを確認し、存在しない所有者のTodoが作成されないよう保証する
	 */
	@Test
	void throwsUserNotFoundExceptionWhenCreatingTodoForUnknownOwner() {
		UUID ownerId = UUID.randomUUID();
		when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> todoService.createTodo(ownerId, "Write tests", null))
				.isInstanceOf(UserNotFoundException.class);

		verify(todoRepository, never()).save(any(Todo.class));
	}
}
