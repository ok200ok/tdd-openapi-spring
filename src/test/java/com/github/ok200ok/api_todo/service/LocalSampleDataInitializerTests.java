package com.github.ok200ok.api_todo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.github.ok200ok.api_todo.config.LocalSampleDataInitializer;
import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.repository.TodoRepository;
import com.github.ok200ok.api_todo.repository.UserRepository;

/**
 * ローカル用サンプルデータ初期化の初回・再実行時の委譲を検証する単体テスト
 * 移行中の実装型をReflectionで参照し、未実装クラスによるテストコンパイル失敗を避ける
 */
@ExtendWith(MockitoExtension.class)
class LocalSampleDataInitializerTests {

	private static final String OWNER_USERNAME = "localdemo";

	@Mock
	private UserRepository userRepository;

	@Mock
	private TodoRepository todoRepository;

	/**
	 * ユーザーが未登録なら作成し、その所有者のサンプルTodoを15件保存する
	 */
	@Test
	void createsOwnerAndFifteenTodosOnFirstInitialization() {
		UUID ownerId = UUID.randomUUID();
		User owner = user(ownerId);
		when(userRepository.findByUserName(OWNER_USERNAME)).thenReturn(Optional.empty());
		when(userRepository.save(any(User.class))).thenReturn(owner);
		when(todoRepository.findAllByUser_IdOrderByUpdatedAtDescIdDesc(ownerId)).thenReturn(List.of());

		LocalSampleDataInitializer initializer =
				new LocalSampleDataInitializer(userRepository, todoRepository, OWNER_USERNAME);
		initializer.initialize();

		verify(userRepository).findByUserName(OWNER_USERNAME);
		verify(userRepository).save(any(User.class));
		ArgumentCaptor<Iterable<Todo>> todosCaptor = ArgumentCaptor.captor();
		verify(todoRepository).saveAll(todosCaptor.capture());
		List<Todo> todos = toList(todosCaptor.getValue());
		assertThat(todos).hasSize(15)
				.allSatisfy(todo -> {
					assertThat(todo.getUser()).isSameAs(owner);
					assertThat(todo.getStatus()).isEqualTo(TodoStatusEnum.PENDING);
				});
	}

	/**
	 * 既存ユーザーでもそのユーザーのTodoだけを置き換えて15件を再作成する
	 */
	@Test
	void replacesOnlyExistingOwnerTodosOnReinitialization() {
		UUID ownerId = UUID.randomUUID();
		User owner = user(ownerId);
		Todo existingTodo = new Todo(owner, "User-created todo", null, TodoStatusEnum.COMPLETED);
		when(userRepository.findByUserName(OWNER_USERNAME)).thenReturn(Optional.of(owner));
		when(todoRepository.findAllByUser_IdOrderByUpdatedAtDescIdDesc(ownerId))
				.thenReturn(List.of(existingTodo));

		LocalSampleDataInitializer initializer =
				new LocalSampleDataInitializer(userRepository, todoRepository, OWNER_USERNAME);
		initializer.initialize();

		verify(userRepository).findByUserName(OWNER_USERNAME);
		verify(userRepository, never()).save(any(User.class));
		verify(todoRepository).deleteAll(List.of(existingTodo));
		ArgumentCaptor<Iterable<Todo>> todosCaptor = ArgumentCaptor.captor();
		verify(todoRepository).saveAll(todosCaptor.capture());
		assertThat(toList(todosCaptor.getValue())).hasSize(15)
				.allSatisfy(todo -> assertThat(todo.getUser()).isSameAs(owner));
	}

	private User user(UUID id) {
		User user = new User(OWNER_USERNAME, "local-only-not-a-login-password", "Local demo", "ROLE_USER");
		ReflectionTestUtils.setField(user, "id", id);
		ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-10-01T12:30:00Z"));
		ReflectionTestUtils.setField(user, "updatedAt", Instant.parse("2026-10-01T12:30:00Z"));
		return user;
	}

	private List<Todo> toList(Iterable<Todo> todos) {
		return java.util.stream.StreamSupport.stream(todos.spliterator(), false).toList();
	}
}
