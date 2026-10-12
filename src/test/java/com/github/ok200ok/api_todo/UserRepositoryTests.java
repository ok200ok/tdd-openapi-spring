package com.github.ok200ok.api_todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.repository.UserRepository;

@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTests {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void savesAndReloadsUserWithGeneratedUuidAndTimestamps() {
		User saved = userRepository.saveAndFlush(user("alice", "Alice Example"));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
		assertThat(saved.getUpdatedAt()).isNotNull();

		User reloaded = userRepository.findById(saved.getId()).orElseThrow();
		assertThat(reloaded.getUserName()).isEqualTo("alice");
		assertThat(reloaded.getPassword()).isEqualTo("{encoded}secret");
		assertThat(reloaded.getDisplayName()).isEqualTo("Alice Example");
		assertThat(reloaded.getRole()).isEqualTo("ROLE_USER");
		assertThat(jdbcTemplate.queryForObject("SELECT version()", String.class))
				.contains("PostgreSQL 17");
	}

	@Test
	void canonicalizesUsernameAndFindsItByCanonicalName() {
		userRepository.saveAndFlush(user("ALICE", null));

		assertThat(userRepository.findByUserName("alice"))
				.map(User::getUserName)
				.contains("alice");
	}

	@Test
	void allowsDuplicateDisplayNames() {
		userRepository.saveAndFlush(user("alice", "Same Name"));
		userRepository.saveAndFlush(user("bob", "Same Name"));

		assertThat(userRepository.count()).isEqualTo(2);
	}

	@Test
	void rejectsDuplicateCanonicalUsernames() {
		userRepository.saveAndFlush(user("alice", null));

		assertThatThrownBy(() -> userRepository.saveAndFlush(user("ALICE", null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsUsernamesContainingNonAlphanumericCharacters() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(user("alice.smith", null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsMissingUsername() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(new User(null, "{encoded}secret", null, "ROLE_USER")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsMissingPassword() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(new User("alice", null, null, "ROLE_USER")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsMissingRole() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(new User("alice", "{encoded}secret", null, null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsUsernameExceedingMaximumLength() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(user("a".repeat(51), null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsPasswordExceedingMaximumLength() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(
				new User("alice", "p".repeat(256), null, "ROLE_USER")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsDisplayNameExceedingMaximumLength() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(
				new User("alice", "{encoded}secret", "d".repeat(51), "ROLE_USER")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsRoleExceedingMaximumLength() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(
				new User("alice", "{encoded}secret", null, "r".repeat(51))))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void allowsDisplayNameToBeNull() {
		User saved = userRepository.saveAndFlush(user("alice", null));

		assertThat(userRepository.findById(saved.getId()).orElseThrow().getDisplayName()).isNull();
	}

	@Test
	void updatesUpdatedAtWhenUserChanges() throws InterruptedException {
		User saved = userRepository.saveAndFlush(user("alice", "Before"));
		LocalDateTime originalUpdatedAt = saved.getUpdatedAt();

		Thread.sleep(10);
		saved.changeDisplayName("After");
		userRepository.flush();

		assertThat(saved.getUpdatedAt()).isAfter(originalUpdatedAt);
	}

	private User user(String userName, String displayName) {
		return new User(userName, "{encoded}secret", displayName, "ROLE_USER");
	}

}
