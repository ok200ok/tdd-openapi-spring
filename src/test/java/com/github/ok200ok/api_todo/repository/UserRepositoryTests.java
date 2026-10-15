package com.github.ok200ok.api_todo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.github.ok200ok.api_todo.TestcontainersConfiguration;
import com.github.ok200ok.api_todo.entity.User;

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

	@Autowired
	private EntityManager entityManager;

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

	/**
	 * Userの2つの監査日時がInstantとしてPostgreSQLのtimestamptz列に保存され、DB再読込後も保たれることを確認する
	 */
	@Test
	void persistsUserTimestampsAsInstantsInPostgres() {
		User saved = userRepository.saveAndFlush(user("alice", "Alice Example"));
		Instant expectedCreatedAt = asInstant(saved.getCreatedAt());
		Instant expectedUpdatedAt = asInstant(saved.getUpdatedAt());
		entityManager.clear();

		User reloaded = userRepository.findById(saved.getId()).orElseThrow();

		assertTimestampMatchesPostgresPrecision(expectedCreatedAt, asInstant(reloaded.getCreatedAt()));
		assertTimestampMatchesPostgresPrecision(expectedUpdatedAt, asInstant(reloaded.getUpdatedAt()));
		assertTimestampColumnIsTimestamptz("users", "created_at");
		assertTimestampColumnIsTimestamptz("users", "updated_at");
	}

	/**
	 * 保存時に大文字を含むuserNameが小文字へ正規化され、その正規化後の値で検索できることを確認する
	 * {@code saveAndFlush}は{@code @PrePersist}を実行させ、Entity内の正規化結果をDBへ反映する
	 * {@code findByUserName}はSpring Data JPAのクエリメソッドで、メソッド名からuserName一致検索を導出する
	 * {@code Optional.map}で検索結果があればEntityからuserNameを取り出し、{@code contains}でaliceが見つかったことを検証する
	 */
	@Test
	void canonicalizesUsernameAndFindsItByCanonicalName() {
		userRepository.saveAndFlush(user("ALICE", null));

		assertThat(userRepository.findByUserName("alice"))
				.map(User::getUserName)
				.contains("alice");
	}

	/**
	 * userNameは一意だがdisplayNameは重複可能というドメイン上の違いを確認する
	 * 異なる一意キーaliceとbobを持つUserに同じ表示名を設定し、両方の保存後に件数が2であることを検証する
	 * {@code count}はRepositoryが提供する件数取得操作であり、表示名に一意制約がないため保存が両方成功する
	 */
	@Test
	void allowsDuplicateDisplayNames() {
		userRepository.saveAndFlush(user("alice", "Same Name"));
		userRepository.saveAndFlush(user("bob", "Same Name"));

		assertThat(userRepository.count()).isEqualTo(2);
	}

	/**
	 * 大文字小文字を正規化した結果、同じuserNameとなるUserを2件保存できないことを確認する
	 * 1件目のaliceをflushしてDBに一意キーを登録し、2件目のALICEは{@code @PrePersist}でaliceとなる
	 * 2件目のflushでusers.user_nameの一意制約違反が発生し、SpringがDataIntegrityViolationExceptionとして通知することを検証する
	 */
	@Test
	void rejectsDuplicateCanonicalUsernames() {
		userRepository.saveAndFlush(user("alice", null));

		assertThatThrownBy(() -> userRepository.saveAndFlush(user("ALICE", null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * userNameは小文字英数字のみ許可するDB CHECK制約が働くことを確認する
	 * Entityの正規化は大文字を小文字に変換するだけで、ピリオドなどの禁止文字を取り除かない
	 * alice.smithをflushし、PostgreSQLのCHECK制約が拒否した結果をDataIntegrityViolationExceptionとして確認する
	 */
	@Test
	void rejectsUsernamesContainingNonAlphanumericCharacters() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(user("alice.smith", null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * userNameが必須であるというJPAマッピングとDBのNOT NULL制約が、永続化時に作用することを確認する
	 * userName以外の必須値は与えたUserを保存し、欠落した項目だけが原因となるようにする
	 * {@code saveAndFlush}でINSERTをDBへ送ると制約違反が発生し、Springのデータアクセス例外{@code DataIntegrityViolationException}として通知されることを検証する
	 * このRepositoryテストはEntityのアプリケーション側入力検証ではなく、永続化先の制約を確認する
	 */
	@Test
	void rejectsMissingUsername() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(new User(null, "{encoded}secret", null, "ROLE_USER")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * passwordが必須であるというJPAマッピングとDBのNOT NULL制約が、永続化時に作用することを確認する
	 * userNameとroleを有効値にし、passwordだけをnullにしたEntityを保存して、DB制約違反を検証する
	 * {@code assertThatThrownBy}はラムダ内の操作が例外を投げることを表明し、続く{@code isInstanceOf}で期待する例外型を確認する
	 */
	@Test
	void rejectsMissingPassword() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(new User("alice", null, null, "ROLE_USER")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * roleが必須であるというJPAマッピングとDBのNOT NULL制約が、永続化時に作用することを確認する
	 * userNameとpasswordを有効値にし、roleだけをnullにして保存することで、この制約だけを検証する
	 * flush時にDBが拒否した結果をSpringの{@code DataIntegrityViolationException}として受け取る
	 */
	@Test
	void rejectsMissingRole() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(new User("alice", "{encoded}secret", null, null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * userName列の最大長が50文字であることを超過値の保存で確認する
	 * {@code "a".repeat(51)}で上限より1文字長い値を作り、他の必須値はヘルパーで有効値にする
	 * {@code saveAndFlush}でDBまで同期し、長さ制約違反がSpringのデータアクセス例外として表面化することを検証する
	 * これは上限超過の拒否だけを検証し、50文字ちょうどが受け入れられる境界値ケースではない
	 */
	@Test
	void rejectsUsernameExceedingMaximumLength() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(user("a".repeat(51), null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * password列の最大長が255文字であることを、256文字の超過値を保存して確認する
	 * userNameとroleは有効値にし、passwordだけを列長より長くすることで失敗原因を限定する
	 * {@code saveAndFlush}によりINSERTをDBへ送り、列長制約違反を検出する
	 * このテストは上限を超えた値の拒否を確認するもので、ハッシュ形式やパスワード検証は対象外
	 */
	@Test
	void rejectsPasswordExceedingMaximumLength() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(
				new User("alice", "p".repeat(256), null, "ROLE_USER")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * 任意のdisplayNameにもVARCHAR(50)の最大長が適用されることを確認する
	 * userName、password、roleを有効値にし、displayNameだけを51文字にしてDBへ保存する
	 * {@code DataIntegrityViolationException}を期待することで、任意項目であることと長さ制約があることを区別する
	 */
	@Test
	void rejectsDisplayNameExceedingMaximumLength() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(
				new User("alice", "{encoded}secret", "d".repeat(51), "ROLE_USER")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * role列の最大長が50文字であることを51文字の値で確認する
	 * その他の必須値は有効に保ち、roleの長さ超過だけで保存が拒否されることを検証する
	 */
	@Test
	void rejectsRoleExceedingMaximumLength() {
		assertThatThrownBy(() -> userRepository.saveAndFlush(
				new User("alice", "{encoded}secret", null, "r".repeat(51))))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * displayNameは任意項目であり、nullのまま保存・取得できることを確認する
	 * 保存後にIDで検索し、再取得したEntityのdisplayNameがnullであることを検証する
	 * このテストはnullableの挙動を対象とし、同一永続化コンテキスト内の検索なのでDBからの再読込までは保証しない
	 */
	@Test
	void allowsDisplayNameToBeNull() {
		User saved = userRepository.saveAndFlush(user("alice", null));

		assertThat(userRepository.findById(saved.getId()).orElseThrow().getDisplayName()).isNull();
	}

	/**
	 * Userの更新でdisplayNameを変更したとき、updatedAtが更新前より後の時刻になることを確認する
	 * {@code saveAndFlush}で初回保存を完了させ、その時点のupdatedAtを比較元として保持する
	 * {@code Thread.sleep(10)}は変更前後の時刻に差が出るよう短い間隔を置く。時刻精度や実行環境に依存するため厳密な長さではなく前後関係だけを検証する
	 * {@code changeDisplayName}でEntityを変更した後、{@code flush}でHibernateに変更をDBへ同期させ、{@code @PreUpdate}の更新時刻設定を発生させる
	 * {@code isAfter}は更新時刻が元の値より後であることを表明する。テスト用トランザクションはcommitされず、他テストへ変更を残さない
	 */
	/**
	 * User更新時はupdatedAtだけが進み、作成時刻createdAtは維持されることを確認する
	 */
	@Test
	void updatesUpdatedAtWhenUserChanges() throws InterruptedException {
		User saved = userRepository.saveAndFlush(user("alice", "Before"));
		Instant originalCreatedAt = asInstant(saved.getCreatedAt());
		Instant originalUpdatedAt = asInstant(saved.getUpdatedAt());

		Thread.sleep(10);
		saved.changeDisplayName("After");
		userRepository.flush();

		assertThat(asInstant(saved.getCreatedAt())).isEqualTo(originalCreatedAt);
		assertThat(asInstant(saved.getUpdatedAt())).isAfter(originalUpdatedAt);
	}

	private Instant asInstant(Object value) {
		assertThat(value).isInstanceOf(Instant.class);
		return Instant.class.cast(value);
	}

	/**
	 * PostgreSQLのマイクロ秒精度を考慮し、永続化前後の時点の差が1マイクロ秒以内であることを確認する
	 */
	private void assertTimestampMatchesPostgresPrecision(Instant expected, Instant actual) {
		assertThat(Duration.between(expected, actual).abs())
				.isLessThanOrEqualTo(Duration.of(1, ChronoUnit.MICROS));
	}

	/**
	 * PostgreSQLのカタログから対象列の型を読み、タイムゾーン付きtimestampであることを確認する
	 */
	private void assertTimestampColumnIsTimestamptz(String tableName, String columnName) {
		String dataType = jdbcTemplate.queryForObject(
				"SELECT data_type FROM information_schema.columns WHERE table_name = ? AND column_name = ?",
				String.class, tableName, columnName);

		assertThat(dataType).isEqualTo("timestamp with time zone");
	}

	private User user(String userName, String displayName) {
		return new User(userName, "{encoded}secret", displayName, "ROLE_USER");
	}

}
