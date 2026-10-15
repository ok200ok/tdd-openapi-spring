package com.github.ok200ok.api_todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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

	/**
	 * Todo IDと所有者IDをRepositoryへ渡し、所有者本人に属するTodoが検索結果として返ることを確認する
	 * {@code when(...).thenReturn(...)}で所有者条件付き検索の結果を設定し、DB検索ではなくServiceの委譲と戻り値を検証する
	 * {@code isSameAs}はServiceがRepositoryから得たTodoを別の値へ置き換えず返すことを確認する
	 */
	@Test
	void findsTodoByIdForOwner() {
		Long todoId = 42L;
		UUID ownerId = UUID.randomUUID();
		Todo todo = new Todo(new User("alice", "hashed-password", null, "ROLE_USER"),
				"Write tests", null, TodoStatusEnum.PENDING);
		when(todoRepository.findByIdAndUser_Id(todoId, ownerId)).thenReturn(Optional.of(todo));

		Todo result = todoService.findTodoByIdForOwner(todoId, ownerId);

		assertThat(result).isSameAs(todo);
		verify(todoRepository).findByIdAndUser_Id(todoId, ownerId);
	}

	/**
	 * Todoが存在しない場合と指定所有者に属さない場合を、同じ未検出結果として扱うことを確認する
	 * 所有者条件付きRepository検索が空を返すよう設定し、Serviceが内部事情を区別せずTodoNotFoundExceptionを通知することを検証する
	 * これにより他の所有者のTodoがあるかどうかを呼び出し元へ漏らさず、APIで404に対応できる契約を保つ
	 */
	@Test
	void throwsTodoNotFoundExceptionWhenTodoIsNotOwnedByUser() {
		Long todoId = 42L;
		UUID ownerId = UUID.randomUUID();
		when(todoRepository.findByIdAndUser_Id(todoId, ownerId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> todoService.findTodoByIdForOwner(todoId, ownerId))
				.isInstanceOf(TodoNotFoundException.class);

		verify(todoRepository).findByIdAndUser_Id(todoId, ownerId);
	}

	/**
	 * 指定した所有者IDでRepositoryを検索し、得られたTodo一覧をServiceが返すことを確認する
	 * Repositoryの応答をMockitoで設定することで、DB検索ではなくServiceの委譲と戻り値に検証範囲を絞る
	 * 一覧の所有者条件と並び順はRepositoryの実DBテストで検証する
	 */
	@Test
	void findsTodosForOwner() {
		UUID ownerId = UUID.randomUUID();
		List<Todo> todos = List.of(
				new Todo(new User("alice", "hashed-password", null, "ROLE_USER"),
						"First", null, TodoStatusEnum.PENDING),
				new Todo(new User("alice", "hashed-password", null, "ROLE_USER"),
						"Second", null, TodoStatusEnum.IN_PROGRESS));
		when(todoRepository.findAllByUser_IdOrderByUpdatedAtDescIdDesc(ownerId)).thenReturn(todos);

		List<Todo> result = todoService.findTodosForOwner(ownerId);

		assertThat(result).isSameAs(todos);
		verify(todoRepository).findAllByUser_IdOrderByUpdatedAtDescIdDesc(ownerId);
	}

	/**
	 * 所有者にTodoがない場合、Serviceが未検出例外ではなく空リストを返すことを確認する
	 * TodoRepositoryの空リストをスタブし、一覧取得で0件が正常な結果として扱われる契約を検証する
	 */
	@Test
	void returnsEmptyListWhenOwnerHasNoTodos() {
		UUID ownerId = UUID.randomUUID();
		when(todoRepository.findAllByUser_IdOrderByUpdatedAtDescIdDesc(ownerId)).thenReturn(List.of());

		assertThat(todoService.findTodosForOwner(ownerId)).isEmpty();
		verify(todoRepository).findAllByUser_IdOrderByUpdatedAtDescIdDesc(ownerId);
	}
}
