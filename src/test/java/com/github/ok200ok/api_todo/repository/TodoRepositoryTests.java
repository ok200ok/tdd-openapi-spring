package com.github.ok200ok.api_todo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.github.ok200ok.api_todo.TestcontainersConfiguration;
import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.entity.User;

@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class TodoRepositoryTests {

	@Autowired
	private TodoRepository todoRepository;

	@Autowired
	private UserRepository userRepository;

	// JPAの永続化コンテキストを操作し、保存済みEntityを1次キャッシュから切り離すために使う
	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Todoの保存時にDB採番IDと作成・更新時刻が設定されることを確認する
	 * TodoはUserを必須とするため、まずUserを保存してから関連付けたTodoを保存する
	 * {@code GenerationType.IDENTITY}ではDBがINSERT時にIDを採番し、{@code saveAndFlush}後にEntityから確認できる
	 * 時刻は{@code @PrePersist}がINSERT前に設定するため、厳密な時刻値でなくnullでないことを検証する
	 * Userを先に保存するのは、Todoのuser_id外部キーが参照できる行を先にDBへ作るため
	 * 最後のOwner ID照合は、Todoの関連が保存したUserを指していることを確認する
	 */
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

	/**
	 * Todoの2つの監査日時がInstantとしてPostgreSQLのtimestamptz列に保存され、DB再読込後も保たれることを確認する
	 */
	@Test
	void persistsTodoTimestampsAsInstantsInPostgres() {
		User user = userRepository.saveAndFlush(user("alice"));
		Todo saved = todoRepository.saveAndFlush(
				new Todo(user, "Write tests", null, TodoStatusEnum.PENDING));
		Instant expectedCreatedAt = asInstant(saved.getCreatedAt());
		Instant expectedUpdatedAt = asInstant(saved.getUpdatedAt());
		entityManager.clear();

		Todo reloaded = todoRepository.findById(saved.getId()).orElseThrow();

		assertTimestampMatchesPostgresPrecision(expectedCreatedAt, asInstant(reloaded.getCreatedAt()));
		assertTimestampMatchesPostgresPrecision(expectedUpdatedAt, asInstant(reloaded.getUpdatedAt()));
		assertTimestampColumnIsTimestamptz("todos", "created_at");
		assertTimestampColumnIsTimestamptz("todos", "updated_at");
	}

	/**
	 * TodoのIDと関連UserのIDを条件に検索し、本人のTodoだけが取得されることをPostgreSQLで確認する
	 * 別のUserも保存して同じTodo IDをそのUser IDと組み合わせ、結果が空になることを検証する
	 * これはSpring Data JPAの派生クエリが関連プロパティuser.idをSQL条件へ反映することを確かめ、Serviceのモックテストでは検証できないDB問い合わせを担保する
	 */
	@Test
	void findsTodoByIdOnlyForItsOwner() {
		User owner = userRepository.saveAndFlush(user("alice"));
		User anotherUser = userRepository.saveAndFlush(user("bob"));
		Todo savedTodo = todoRepository.saveAndFlush(
				new Todo(owner, "Write tests", null, TodoStatusEnum.PENDING));
		entityManager.clear();

		assertThat(todoRepository.findByIdAndUser_Id(savedTodo.getId(), owner.getId()))
				.map(Todo::getId)
				.contains(savedTodo.getId());
		assertThat(todoRepository.findByIdAndUser_Id(savedTodo.getId(), anotherUser.getId()))
				.isEmpty();
	}

	/**
	 * 所有者別一覧が他UserのTodoを含めず、更新日時降順、同時刻ではID降順で返ることをPostgreSQLで確認する
	 * 各TodoをRepositoryから保存した後、JdbcTemplateでupdated_atを既知の値に設定し、時刻差と同時刻のID順を決定的に検証する
	 * 別所有者にはより新しい日時を設定して、所有者条件が並び順に埋もれず適用されることも確認する
	 */
	@Test
	void findsTodosByOwnerOrderedByUpdatedAtAndIdDescending() {
		User owner = userRepository.saveAndFlush(user("alice"));
		User anotherUser = userRepository.saveAndFlush(user("bob"));
		Todo oldest = todoRepository.saveAndFlush(
				new Todo(owner, "Oldest", null, TodoStatusEnum.PENDING));
		Todo tiedFirst = todoRepository.saveAndFlush(
				new Todo(owner, "Tied first", null, TodoStatusEnum.PENDING));
		Todo tiedSecond = todoRepository.saveAndFlush(
				new Todo(owner, "Tied second", null, TodoStatusEnum.PENDING));
		Todo anotherOwnersTodo = todoRepository.saveAndFlush(
				new Todo(anotherUser, "Another owner", null, TodoStatusEnum.PENDING));
		LocalDateTime olderTime = LocalDateTime.of(2025, 1, 1, 10, 0);
		LocalDateTime newerTime = LocalDateTime.of(2025, 1, 2, 10, 0);
		LocalDateTime newestTime = LocalDateTime.of(2025, 1, 3, 10, 0);
		jdbcTemplate.update("UPDATE todos SET updated_at = ? WHERE id = ?", olderTime, oldest.getId());
		jdbcTemplate.update("UPDATE todos SET updated_at = ? WHERE id = ?", newerTime, tiedFirst.getId());
		jdbcTemplate.update("UPDATE todos SET updated_at = ? WHERE id = ?", newerTime, tiedSecond.getId());
		jdbcTemplate.update("UPDATE todos SET updated_at = ? WHERE id = ?", newestTime, anotherOwnersTodo.getId());
		entityManager.clear();

		List<Todo> todos = todoRepository.findAllByUser_IdOrderByUpdatedAtDescIdDesc(owner.getId());

		assertThat(todos)
				.extracting(Todo::getId)
				.containsExactly(tiedSecond.getId(), tiedFirst.getId(), oldest.getId());
	}

	/**
	 * Todoのタイトル、null説明、状態がDB保存後にJPA経由で再取得できることを確認する
	 * {@code saveAndFlush}後に{@code entityManager.clear()}で現在の永続化コンテキストにあるEntityをすべて切り離し、{@code findById}がDBから読み直すようにする
	 * {@code orElseThrow()}は検索結果の{@code Optional}が空ならテストを失敗させ、対象Todoが存在しない状態を見逃さない
	 * titleとdescriptionは列マッピングを、statusはEnumからの復元を検証する。実際のDB列がEnum名の文字列かは別テストでSQLを直接確認する
	 * 最後のUser名取得は関連アクセスを伴うため、その関連の検証目的は次のリレーションタスクで扱う
	 */
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

	/**
	 * Todo検索でUser関連を即時取得せず、関連プロパティへアクセスしたときに利用可能となることを確認する
	 * {@code entityManager.clear()}で保存時のEntityを永続化コンテキストから外し、Repository検索による取得状態を観察する
	 * {@code Persistence.getPersistenceUtil().isLoaded(entity, "user")}はJPA標準APIで関連がロード済みかを調べる。直後がfalseであることからLAZY設定を確認する
	 * その後userNameへアクセスし値を検証することで、遅延ロードされた関連を実際に利用できることを確認する
	 * このアクセスはEntityManagerが有効なテストトランザクション内で行っている。トランザクション終了後の関連アクセス可否はこのテストでは扱わない
	 */
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

	/**
	 * Userとの関連は除外し、16,000文字のTodo説明をPostgreSQLのTEXT列へ保存できることを確認する
	 * {@code repeat(2_000)}で8文字の文字列を繰り返して長文を作り、保存値と同じ内容であることを検証する
	 * {@code saveAndFlush}はDBへのINSERTまで進めるため、Javaオブジェクト内の値比較だけでなく永続化処理を通過したことを確認できる
	 * Entityの{@code columnDefinition = "text"}はHibernateが生成するDDLでPostgreSQLのTEXT型を指定し、短いVARCHAR列長で拒否されないことを確かめる
	 * TEXTに固定のVARCHAR上限がないことは、DB容量や実用上の格納量が無制限という意味ではない
	 */
	@Test
	void allowsLongDescriptionText() {
		User user = userRepository.saveAndFlush(user("alice"));
		String description = "details ".repeat(2_000);

		Todo saved = todoRepository.saveAndFlush(
				new Todo(user, "Write tests", description, TodoStatusEnum.PENDING));

		assertThat(saved.getDescription()).isEqualTo(description);
	}

	/**
	 * Java Enumの全状態がDBでも定数名の文字列として保存されることを確認する
	 * {@code TodoStatusEnum.values()}で定義済みの全状態を列挙し、それぞれをTodoのstatusに指定して保存する
	 * {@code JdbcTemplate.queryForList}はRepositoryやEntityの変換を通さずSQL結果を直接読むため、DB列の実際の値を確認できる
	 * {@code containsExactly}は値と順序の両方を検証し、Enum名が定義順どおりに保存されたことを確かめる
	 */
	@Test
	void persistsAllStatusesByTheirEnumNames() {
		User user = user("alice");
		for (TodoStatusEnum status : TodoStatusEnum.values()) {
			todoRepository.saveAndFlush(new Todo(user, status.name(), null, status));
		}

		assertThat(jdbcTemplate.queryForList("SELECT status FROM todos ORDER BY id", String.class))
				.containsExactly("PENDING", "IN_PROGRESS", "COMPLETED");
	}

	/**
	 * Todoの状態変更でupdatedAtが更新前より後になることを確認する
	 * 作成時刻と更新時刻を初期化したTodoを保存し、更新時刻を比較元に保持する
	 * 短い待機後にstatusを変更し、flushでUPDATEと{@code @PreUpdate}を発生させる。時刻の厳密値でなく前後関係だけを検証する
	 * {@code isAfter}で新しい更新時刻が比較元より後であることを表明する
	 */
	/**
	 * Todo更新時はupdatedAtだけが進み、作成時刻createdAtは維持されることを確認する
	 */
	@Test
	void updatesUpdatedAtWhenTodoChanges() throws InterruptedException {
		User user = user("alice");
		Todo todo = todoRepository.saveAndFlush(
				new Todo(user, "Write tests", null, TodoStatusEnum.PENDING));
		Instant originalCreatedAt = asInstant(todo.getCreatedAt());
		Instant originalUpdatedAt = asInstant(todo.getUpdatedAt());

		Thread.sleep(10);
		todo.changeStatus(TodoStatusEnum.COMPLETED);
		todoRepository.flush();

		assertThat(asInstant(todo.getCreatedAt())).isEqualTo(originalCreatedAt);
		assertThat(asInstant(todo.getUpdatedAt())).isAfter(originalUpdatedAt);
	}

	/**
	 * Todoの所有者Userが必須であり、OwnerなしのTodoをDBへ保存できないことを確認する
	 * 他の必須フィールドには有効値を指定し、userだけをnullにして関連の必須性を単独で検証する
	 * {@code saveAndFlush}でINSERTをDBへ送り、{@code user_id}のNOT NULL制約違反がSpringの{@code DataIntegrityViolationException}として通知されることを確認する
	 * このケースは所有者の認可ではなく、TodoにOwnerが存在するというデータ整合性を検証する
	 */
	@Test
	void rejectsMissingTodoOwner() {
		assertThatThrownBy(() -> todoRepository.saveAndFlush(
				new Todo(null, "Write tests", null, TodoStatusEnum.PENDING)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * titleがDB上で必須であり、nullのTodoを保存できないことを確認する
	 * Userとstatusは有効値にし、titleだけをnullにすることでNOT NULL制約の検証対象を一つに限定する
	 * {@code saveAndFlush}でINSERTをDBへ送り、違反がSpringの{@code DataIntegrityViolationException}として通知されることを確認する
	 */
	@Test
	void rejectsMissingTodoTitle() {
		User user = user("alice");
		assertThatThrownBy(() -> todoRepository.saveAndFlush(
				new Todo(user, null, null, TodoStatusEnum.PENDING)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * statusがDB上で必須であり、nullのTodoを保存できないことを確認する
	 * Userとtitleを有効値に保ち、statusだけをnullにして永続化する
	 * Enumの許可値ではなく、まずNOT NULL制約による必須性を検証するケース
	 */
	@Test
	void rejectsMissingTodoStatus() {
		User user = user("alice");
		assertThatThrownBy(() -> todoRepository.saveAndFlush(
				new Todo(user, "Write tests", null, null)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * title列の最大長が100文字であり、101文字の値を保存できないことを確認する
	 * Userとstatusは有効値で用意し、タイトル長だけを上限超過させる
	 * {@code saveAndFlush}でDBへ反映し、列長制約違反を{@code DataIntegrityViolationException}として検証する
	 * このケースは超過値の拒否を確認するもので、100文字ちょうどが受け入れられることまでは検証しない
	 */
	@Test
	void rejectsTitleExceedingMaximumLength() {
		User user = userRepository.saveAndFlush(user("alice"));

		assertThatThrownBy(() -> todoRepository.saveAndFlush(
				new Todo(user, "t".repeat(101), null, TodoStatusEnum.PENDING)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	/**
	 * TodoがUser行を外部キー参照しており、User削除時にTodoを連鎖削除せず削除を拒否することを確認する
	 * まずPostgreSQLのシステムカタログpg_constraintを直接問い合わせ、todosテーブルに外部キー制約が定義されていることを確認する
	 * {@code entityManager.clear()}でUserとTodoを永続化コンテキストから切り離してからUserを削除し、JPAのメモリ上の状態ではなくDB上の参照制約を検証する
	 * {@code flush()}時に参照中UserのDELETEが外部キー制約で拒否され、Springの{@code DataIntegrityViolationException}となることを確認する
	 * テストは削除後のTodo行数を直接照合していないため、連鎖削除されないことは削除失敗と外部キーの存在から確認する範囲に留まる
	 */
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

}
