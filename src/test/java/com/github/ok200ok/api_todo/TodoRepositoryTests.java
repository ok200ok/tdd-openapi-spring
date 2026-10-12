package com.github.ok200ok.api_todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.repository.TodoRepository;
import com.github.ok200ok.api_todo.repository.UserRepository;

@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class TodoRepositoryTests {

	@Autowired
	private TodoRepository todoRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void savesTodoWithGeneratedIdAndRequiredUser() {
		User user = userRepository.saveAndFlush(user("alice"));
		Todo saved = todoRepository.saveAndFlush(
				new Todo(user, "Write tests", "Cover persistence behavior", TodoStatusEnum.IN_PROGRESS));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();
		assertThat(saved.getUser().getId()).isEqualTo(user.getId());
	}

	@Test
	void reloadsAllTodoFieldsAndEnumAsItsStringValue() {
		User user = userRepository.saveAndFlush(user("alice"));
		Todo saved = todoRepository.saveAndFlush(
				new Todo(user, "Write tests", null, TodoStatusEnum.COMPLETED));
		entityManager.clear();

		Todo reloaded = todoRepository.findById(saved.getId()).orElseThrow();

		assertThat(reloaded.getTitle()).isEqualTo("Write tests");
		assertThat(reloaded.getDescription()).isNull();
		assertThat(reloaded.getStatus()).isEqualTo(TodoStatusEnum.COMPLETED);
		assertThat(reloaded.getUser().getUserName()).isEqualTo("alice");
	}

	@Test
	void keepsUserAssociationLazyUntilAccessed() {
		User user = userRepository.saveAndFlush(user("alice"));
		Todo saved = todoRepository.saveAndFlush(
				new Todo(user, "Write tests", null, TodoStatusEnum.PENDING));
		entityManager.clear();

		Todo reloaded = todoRepository.findById(saved.getId()).orElseThrow();

		assertThat(Persistence.getPersistenceUtil().isLoaded(reloaded, "user")).isFalse();
		assertThat(reloaded.getUser().getUserName()).isEqualTo("alice");
	}

	@Test
	void allowsLongDescriptionText() {
		User user = userRepository.saveAndFlush(user("alice"));
		String description = "details ".repeat(2_000);

		Todo saved = todoRepository.saveAndFlush(
				new Todo(user, "Write tests", description, TodoStatusEnum.PENDING));

		assertThat(saved.getDescription()).isEqualTo(description);
	}

	@Test
	void persistsAllStatusesByTheirEnumNames() {
		User user = user("alice");
		for (TodoStatusEnum status : TodoStatusEnum.values()) {
			todoRepository.saveAndFlush(new Todo(user, status.name(), null, status));
		}

		assertThat(jdbcTemplate.queryForList("SELECT status FROM todos ORDER BY id", String.class))
				.containsExactly("PENDING", "IN_PROGRESS", "COMPLETED");
	}

	@Test
	void updatesUpdatedAtWhenTodoChanges() throws InterruptedException {
		User user = user("alice");
		Todo todo = todoRepository.saveAndFlush(
				new Todo(user, "Write tests", null, TodoStatusEnum.PENDING));
		LocalDateTime originalUpdatedAt = todo.getUpdatedAt();

		Thread.sleep(10);
		todo.changeStatus(TodoStatusEnum.COMPLETED);
		todoRepository.flush();

		assertThat(todo.getUpdatedAt()).isAfter(originalUpdatedAt);
	}

	@Test
	void rejectsMissingTodoOwner() {
		assertThatThrownBy(() -> todoRepository.saveAndFlush(
				new Todo(null, "Write tests", null, TodoStatusEnum.PENDING)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsMissingTodoTitle() {
		User user = user("alice");
		assertThatThrownBy(() -> todoRepository.saveAndFlush(
				new Todo(user, null, null, TodoStatusEnum.PENDING)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsMissingTodoStatus() {
		User user = user("alice");
		assertThatThrownBy(() -> todoRepository.saveAndFlush(
				new Todo(user, "Write tests", null, null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsTitleExceedingMaximumLength() {
		User user = userRepository.saveAndFlush(user("alice"));

		assertThatThrownBy(() -> todoRepository.saveAndFlush(
				new Todo(user, "t".repeat(101), null, TodoStatusEnum.PENDING)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void doesNotDeleteTodosWhenTheirUserIsDeleted() {
		User user = user("alice");
		todoRepository.saveAndFlush(new Todo(user, "Write tests", null, TodoStatusEnum.PENDING));

		assertThat(jdbcTemplate.queryForObject(
				"SELECT count(*) FROM pg_constraint WHERE conrelid = 'todos'::regclass AND contype = 'f'",
				Integer.class)).isEqualTo(1);

		entityManager.clear();
		userRepository.deleteById(user.getId());

		assertThatThrownBy(userRepository::flush)
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private User user(String userName) {
		return userRepository.saveAndFlush(new User(userName, "{encoded}secret", null, "ROLE_USER"));
	}

}
