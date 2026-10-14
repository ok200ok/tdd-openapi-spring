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

/**
 * UserのJPAマッピングとUserRepositoryの永続化操作を、テスト用データベースで確認する
 * {@code @DataJpaTest}はJPA関連のレイヤーコンポーネントを中心にテスト環境を構成し、各テストを通常トランザクション内で実行して終了時にロールバックする
 * テストからDBまでの呼び出し経路は、UserRepository、Spring Data JPAの実装、JPAのEntityManager、Hibernate、JDBCを経てTestcontainersのPostgreSQLへ至る
 * {@code showSql = false}はHibernateが生成するSQLをログに出さない指定で、テスト出力を読みやすく保つ
 * {@code spring.jpa.hibernate.ddl-auto=create-drop}はEntity定義からテスト用スキーマを作成し、テストコンテキスト終了時に破棄する指定
 */
@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
// @DataJpaTest既定の組み込みDBへの差し替えを無効にし、TestcontainersのPostgreSQLを使う
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// PostgreSQLコンテナとSpring Bootの接続設定を定義するテスト構成を読み込む
@Import(TestcontainersConfiguration.class)
class UserRepositoryTests {

	// Springテストコンテキストが用意したUserRepository実装を注入し、テスト対象の永続化操作に使う
	@Autowired
	private UserRepository userRepository;

	// Repositoryを介さずSQLを実行し、実際の接続先DBのバージョンを確かめる
	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * UserRepositoryを通じて保存操作とID検索を行い、Entityの基本情報と接続先DBを確認する
	 * {@code save}はRepositoryに保存を依頼し、EntityをJPAの永続化コンテキストに登録する
	 * {@code saveAndFlush}はsaveに加えて保留中の変更をDBへ同期するため、この時点でSQLやDB制約の問題を検出できる。ただしトランザクションをcommitするわけではない
	 * 続くID検索はRepositoryの検索操作を確認する。保存したEntityが同じ永続化コンテキスト内に残っているため、DBからの再読込そのものを保証するテストではない
	 * IDと時刻の検証は保存時の値設定を、各フィールドの検証は保存対象の値を、PostgreSQLのバージョン検証はTestcontainersの接続先を確認する
	 */
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
		// Repository経由の保存確認とは別に、SQLを直接実行して実際の接続先がPostgreSQL 17であることを検証する
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
