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

	@Test
	void throwsUserNotFoundExceptionWhenCreatingTodoForUnknownOwner() {
		UUID ownerId = UUID.randomUUID();
		when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> todoService.createTodo(ownerId, "Write tests", null))
				.isInstanceOf(UserNotFoundException.class);

		verify(todoRepository, never()).save(any(Todo.class));
	}
}
