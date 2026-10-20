# テストケース一覧

各タスクで検討・合意したテストケースと期待結果を集約するテストケース一覧カタログ。テストの有無だけでなく、「何の仕様を、どのレイヤーコンポーネントで、なぜ検証するのか」を後から追えるようにする。

## 記載・更新ルール

- タスクのテストケースを開発者と合意した時点で、実装や解説に進む前に一覧へ記載する。
- 各テストケース表の状態欄には、凡例に定義した対応する絵文字だけを記載する。説明文は凡例に置き、状態欄へ文字を併記しない。
- 新規テストの追加・修正時はテスト名、期待結果、リンクを同期する。既存ケースと異なる仕様を検証する場合はケースを分ける。
- テストメソッド名のリンクはソースファイルと行番号へ結び付ける。コード変更で行番号や名前が変わった場合はリンクを更新する。
- 各タスクの完了前に、合意済みケースの記載漏れ、期待結果、リンク、状態を確認する。
- 後続タスクの具体的なケースは仕様合意時に決める。未合意のテスト内容を先回りして仕様として固定しない。
- Section 3以降は一般的に必ず担保すべき基本要件に絞り、重要な振る舞いを少数の明確なケースで検証する。網羅性を目的にケースやテストコードを増やさず、Section 2の既存記録は変更しない。

## 状態

| 状態 | 意味 |
| --- | --- |
| 🚧未検討 | 対象タスクの仕様・ケースをまだ協議していない |
| 🤝合意済み | 仕様、ケース、期待結果に合意したが、対象テストコードや実装内容の確認は未実施|
| ✅実行確認済み | 開発者がテストを実行し、その結果と期待結果の一致を確認した |

## Section 2: JPA / Repository

テスト実行状態は開発者が結果を共有した場合にのみ更新する。

### タスク2: Entity・RepositoryとPostgreSQL Testcontainers

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| Repository経由のUser保存とID検索、生成値、DB接続先 | 保存時にID・作成/更新時刻が設定され、指定したUser情報を取得できる。`SELECT version()`でPostgreSQL 17への接続を確認する。検索は同一永続化コンテキスト内で行うためDBからの再読込は保証しない | [`savesAndReloadsUserWithGeneratedUuidAndTimestamps`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L49) | ✅ |

### タスク3: Userの基本マッピング

上限ちょうどの値を許すことなど、記載のない境界値まではこの合意に含めない。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| UUIDと作成時タイムスタンプ | 新規保存時にUUID、`createdAt`、`updatedAt`が設定される | [`savesAndReloadsUserWithGeneratedUuidAndTimestamps`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L49) | ✅ |
| 必須の`userName` | `userName`がないUserの保存がNOT NULL制約により拒否され、Springのデータアクセス例外として通知される | [`rejectsMissingUsername`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L125) | ✅ |
| 必須の`password` | `password`がないUserの保存がNOT NULL制約により拒否され、Springのデータアクセス例外として通知される | [`rejectsMissingPassword`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L136) | ✅ |
| 必須の`role` | `role`がないUserの保存がNOT NULL制約により拒否され、Springのデータアクセス例外として通知される | [`rejectsMissingRole`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L147) | ✅ |
| 任意の`displayName` | `displayName`がnullでも保存でき、取得後もnullである | [`allowsDisplayNameToBeNull`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L206) | ✅ |
| `userName`の最大長超過 | 51文字のユーザー名を保存するとDB制約違反になる | [`rejectsUsernameExceedingMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L159) | ✅ |
| `password`の最大長超過 | 256文字のパスワード文字列を保存するとDB制約違反になる | [`rejectsPasswordExceedingMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L171) | ✅ |
| `displayName`の最大長超過 | 51文字の表示名を保存するとDB制約違反になる | [`rejectsDisplayNameExceedingMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L183) | ✅ |
| `role`の最大長超過 | 51文字のRoleを保存するとDB制約違反になる | [`rejectsRoleExceedingMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L194) | ✅ |
| 更新時の`updatedAt` | 表示名変更をflushすると、更新時刻が変更前より後になる | [`updatesUpdatedAtWhenUserChanges`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L220) | ✅ |

### タスク4: `userName`の正規化・形式・一意性・検索

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 小文字への正規化と検索 | 大文字を含むユーザー名を保存するとEntityが小文字へ正規化し、小文字の名前で検索できる | [`canonicalizesUsernameAndFindsItByCanonicalName`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L73) | ✅ |
| 正規化後の一意性 | 大文字小文字だけが異なる2件目を保存すると正規化後の名前が重複し、DBの一意制約で拒否される | [`rejectsDuplicateCanonicalUsernames`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L100) | ✅ |
| 許容文字 | 小文字英数字以外を含む名前はEntity正規化だけでは除去されず、DBのCHECK制約で拒否される | [`rejectsUsernamesContainingNonAlphanumericCharacters`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L113) | ✅ |
| 表示名の重複許可 | 異なるユーザー名のUser 2件に同じ`displayName`を設定しても、両方保存できる | [`allowsDuplicateDisplayNames`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L87) | ✅ |

### タスク5: Todoの基本マッピング

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 採番IDと作成時刻 | Todo保存時にDB採番IDと作成/更新時刻が設定される。Ownerの関連ID検証はタスク6で扱う | [`savesTodoWithGeneratedIdAndRequiredUser`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L51) | ✅ |
| 各フィールドとEnum文字列保存 | Todoのタイトル、任意の説明、状態を保存・再取得でき、各Enum名を文字列としてDBに保存する | [`reloadsAllTodoFieldsAndEnumAsItsStringValue`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L70)、[`persistsAllStatusesByTheirEnumNames`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L129) | ✅ |
| 長い説明文 | `description`に長文を保存できる | [`allowsLongDescriptionText`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L112) | ✅ |
| 更新時の`updatedAt` | 状態変更をflushすると、更新時刻が変更前より後になる | [`updatesUpdatedAtWhenTodoChanges`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L146) | ✅ |
| 必須のタイトルと状態 | `title`または`status`を欠くTodoの保存がDB制約違反になる | [`rejectsMissingTodoTitle`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L178)、[`rejectsMissingTodoStatus`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L191) | ✅ |
| タイトルの最大長 | 101文字のタイトルを保存するとDB制約違反になる | [`rejectsTitleExceedingMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L205) | ✅ |

### タスク6: TodoとUserのリレーション

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 必須Ownerと関連ID | 保存前にUserを永続化し、Todoの関連がそのUser IDを参照することを確認する。OwnerがnullのTodoはDB制約で拒否される。所有者別検索や認可はこのRepositoryテストでは検証しない | [`savesTodoWithGeneratedIdAndRequiredUser`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L51)、[`rejectsMissingTodoOwner`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L166) | ✅ |
| 遅延ロード | DBから再取得した直後はTodoのuser関連が未ロードであり、userNameへのアクセス時に関連を利用できる | [`keepsUserAssociationLazyUntilAccessed`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L92) | ✅ |
| 外部キーと削除制約 | todos.user_idの外部キーが存在し、参照中Userの削除がDB制約で拒否される。テストはTodo行数の事後確認までは行わない | [`doesNotDeleteTodosWhenTheirUserIsDeleted`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L221) | ✅ |

### タスク7: Section 2全体のテストと設計照合

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| Section 2のRepositoryテストと既存結合テスト | `.\gradlew.bat test`で全27件（UserRepositoryTests 14件、TodoRepositoryTests 11件、ApiTodoApplicationTests 2件）が失敗・エラーなく成功する。Repositoryテストの結果をデータモデル文書と各ケースの期待結果に照らし、カタログ・READMEの記載と一致することを確認する | [UserRepositoryTests](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java)、[TodoRepositoryTests](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java)、[ApiTodoApplicationTests](../src/test/java/com/github/ok200ok/api_todo/ApiTodoApplicationTests.java) | ✅ |

## Section 3: ドメイン・ビジネスロジック層（`Service`）の `TDD` 実装

後続タスクのテストケースは、仕様と検証境界を開発者と合意した時点で具体的な観点・期待結果・テストリンクを追記する。ケース内容を先回りして固定しない。

### タスク: Todo作成のService単体テスト

MockitoでRepositoryを置き換え、TodoServiceの作成処理を検証する。DBクエリやSpringコンテキストはこの単体テストの対象外とする。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 所有者・内容・初期状態を指定したTodo作成 | `ownerId`から取得したUser、指定したタイトルと説明、初期状態`PENDING`を持つTodoがTodoRepositoryへ保存され、保存結果がServiceから返る | [`createsTodoForOwnerWithPendingStatus`](../src/test/java/com/github/ok200ok/api_todo/service/TodoServiceTests.java#L54) | ✅ |
| 存在しない所有者での作成 | UserRepositoryに所有者が見つからない場合、`UserNotFoundException`で失敗し、TodoRepositoryへ保存要求を行わない | [`throwsUserNotFoundExceptionWhenCreatingTodoForUnknownOwner`](../src/test/java/com/github/ok200ok/api_todo/service/TodoServiceTests.java#L79) | ✅ |

### タスク: 所有者を指定したTodoのID検索

ServiceはTodo IDと所有者IDを受け取り、所有者条件付きのRepository検索結果を返す。存在しないTodoと他の所有者のTodoは同じ`TodoNotFoundException`として扱い、Service単体テストとRepositoryの実DBテストでそれぞれの責務を検証する。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 所有者に属するTodoの検索 | 指定したTodo IDと所有者IDに一致するTodoが返る | [`findsTodoByIdForOwner`](../src/test/java/com/github/ok200ok/api_todo/service/TodoServiceTests.java#L95) | ✅ |
| 不存在・別所有者のTodo検索 | Repository検索が空なら`TodoNotFoundException`となる。対象が存在しない場合と別所有者の場合を同一の結果として扱う | [`throwsTodoNotFoundExceptionWhenTodoIsNotOwnedByUser`](../src/test/java/com/github/ok200ok/api_todo/service/TodoServiceTests.java#L114) | ✅ |
| Repositoryの所有者条件 | PostgreSQL上で所有者本人のTodoはIDと所有者IDの組み合わせで検索でき、同じTodo IDを別所有者IDで検索すると結果が空になる | [`findsTodoByIdOnlyForItsOwner`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L68) | ✅ |

### タスク: 所有者別Todo一覧

Serviceは所有者IDをRepositoryへ渡して一覧取得を委譲する。0件は通常の検索結果として空リストを返す。順序は`updated_at`降順、同時刻の場合は`id`降順とする。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 所有者別一覧の取得 | Serviceが所有者IDをRepositoryへ渡し、返された一覧を返す | [`findsTodosForOwner`](../src/test/java/com/github/ok200ok/api_todo/service/TodoServiceTests.java#L132) | ✅ |
| 該当Todoがない一覧 | Repositoryが空の結果を返す場合、Serviceも空リストを返す | [`returnsEmptyListWhenOwnerHasNoTodos`](../src/test/java/com/github/ok200ok/api_todo/service/TodoServiceTests.java#L152) | ✅ |
| Repositoryの所有者条件と順序 | PostgreSQL上で指定所有者のTodoのみを返し、`updated_at`降順、同時刻ではID降順になる | [`findsTodosByOwnerOrderedByUpdatedAtAndIdDescending`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L89) | ✅ |

### タスク: Todo更新

Serviceは所有者条件付きでTodoを取得し、タイトル・説明・状態をひとまとまりで変更して更新後のTodoを返す。Repositoryの所有者条件は前タスクの実DBテストで確認済みのため重複せず、トランザクションによるdirty checkingの実動作はSection 6の結合テストで扱う。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 所有者本人のTodo更新 | タイトル・説明・状態のすべてが変更され、更新後のTodoが返る | [`updatesTodoForOwner`](../src/test/java/com/github/ok200ok/api_todo/service/TodoServiceTests.java#L166) | ✅ |
| 不存在・別所有者のTodo更新 | 所有者条件付き検索が空の場合は`TodoNotFoundException`となり、Entityの変更操作は行われない | [`throwsTodoNotFoundExceptionWhenUpdatingTodoNotOwnedByUser`](../src/test/java/com/github/ok200ok/api_todo/service/TodoServiceTests.java#L188) | ✅ |

### タスク: Todo削除

Serviceは所有者条件付きでTodoを取得してから削除する。不存在と所有者不一致は同じ`TodoNotFoundException`として扱い、他ユーザーのTodoを削除しない。Repositoryの所有者条件は前タスクの実DBテストを再利用する。Service単体テストでは削除メソッドの呼び出しを確認し、JPAによる実DB削除はSection 6の結合テストで確認する。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 所有者本人のTodo削除 | 所有者条件付きで取得したTodoがRepositoryへ削除依頼され、Serviceは正常終了する | [`deletesTodoForOwner`](../src/test/java/com/github/ok200ok/api_todo/service/TodoServiceTests.java#L215) | ✅ |
| 不存在・別所有者のTodo削除 | 所有者条件付き検索が空の場合は`TodoNotFoundException`となり、Repositoryへ削除依頼しない | [`throwsTodoNotFoundExceptionWhenDeletingTodoNotOwnedByUser`](../src/test/java/com/github/ok200ok/api_todo/service/TodoServiceTests.java#L233) | ✅ |

## Section 4: API層（`Controller`）と `OpenAPI`

### タスク2: UTC基準の日時型への見直し

UserとTodoの`createdAt`／`updatedAt`を`Instant`で扱い、PostgreSQLの`timestamptz`へ保存する。RepositoryテストではPostgreSQL Testcontainersを使い、保存後に永続化コンテキストをクリアしてDBから再読込する。テストメソッドのリンクはテストコード作成後に追加する。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| Userの作成・更新日時の永続化 | Userの`createdAt`と`updatedAt`が設定され、DBから再読込した値が保存前の`Instant`と1マイクロ秒以内で一致する。PostgreSQLの両カラムが`timestamptz`であることも確認する | [`persistsUserTimestampsAsInstantsInPostgres`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L77) | ✅ |
| Todoの作成・更新日時の永続化 | Todoの`createdAt`と`updatedAt`が設定され、DBから再読込した値が保存前の`Instant`と1マイクロ秒以内で一致する。PostgreSQLの両カラムが`timestamptz`であることも確認する | [`persistsTodoTimestampsAsInstantsInPostgres`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L71) | ✅ |
| User更新時の日時 | User更新時に`updatedAt`が更新前より後になり、`createdAt`は変化しない | [`updatesUpdatedAtWhenUserChanges`](../src/test/java/com/github/ok200ok/api_todo/repository/UserRepositoryTests.java#L248) | ✅ |
| Todo更新時の日時 | Todo更新時に`updatedAt`が更新前より後になり、`createdAt`は変化しない | [`updatesUpdatedAtWhenTodoChanges`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L227) | ✅ |

### タスク3: Todo一覧のDBページネーション

Repositoryは所有者条件・ページ分割・`updatedAt`降順、同時刻ではID降順の順序をPostgreSQL上で適用する。Serviceは所有者IDとページ指定をRepositoryへ委譲し、総件数を含む`Page<Todo>`を返す。RepositoryはTestcontainersのPostgreSQL、ServiceはMockito単体テストで検証する。公開APIの1始まりページ番号や不正なHTTPクエリ値の`400`応答は後続のControllerタスクで扱う。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| Repositoryの所有者別複数ページ取得 | 所有者のTodoを複数ページに分割し、他所有者のTodoを含めない。各ページは`updatedAt`降順、同時刻ではID降順とし、`totalElements`と`totalPages`も正しい | [`findsOwnerTodosAcrossPagesWithStableOrderAndPageTotals`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L146) | ✅ |
| Repositoryの範囲外ページ | 最終ページより後のページは内容が空になるが、総件数と総ページ数は実データに基づく値を返す | [`returnsEmptyPageBeyondLastOwnerTodoPageWithActualTotals`](../src/test/java/com/github/ok200ok/api_todo/repository/TodoRepositoryTests.java#L180) | ✅ |
| Serviceのページ検索委譲 | Serviceが指定所有者IDと`Pageable`をRepositoryへ渡し、Repositoryから返された`Page<Todo>`を返す | [`findsTodosForOwnerPage`](../src/test/java/com/github/ok200ok/api_todo/service/TodoServiceTests.java#L176) | ✅ |

### タスク4: DTO・Validationの詳細設計

DTO項目と入力検証のHTTP契約は[DTO・Validation設計仕様](dto-validation.md)のとおり合意済み。以下はタスク4で合意したケースで、タスク5の`@WebMvcTest`で一度だけ検証する。タスク5側では同じDTO制約や応答フィールドを重複ケースとして登録せず、HTTP操作・Service連携・エラー変換を追加で確認する。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 作成・更新DTOの正常入力 | 作成DTOはタイトルと任意説明を受け付け、状態・ユーザーIDを持たない。更新DTOはタイトル・説明・状態を受け付ける。タイトル前後の空白は除去される | [`validatesTitleAndAcceptsTrimmedMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerInputValidationTests.java#L66)、[`trimsAndValidatesTitleOnUpdate`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerUpdateValidationTests.java#L81) | ✅ |
| タイトルの検証 | 欠落・`null`・空白のみを拒否し、trim後101文字を拒否する。trim後100文字は受け入れる | [`validatesTitleAndAcceptsTrimmedMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerInputValidationTests.java#L66)、[`trimsAndValidatesTitleOnUpdate`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerUpdateValidationTests.java#L81) | ✅ |
| 説明の省略・null | 作成時は説明省略を許容する。PUTでは説明の省略または`null`を既存説明の削除として扱う。説明長はこのAPIでは制限しない | [`acceptsOptionalNullAndLongDescriptionOnCreate`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerInputValidationTests.java#L111)、[`treatsOmittedAndNullDescriptionAsRemovalAndAllowsLongDescription`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerUpdateValidationTests.java#L114) | ✅ |
| 状態と未知JSONフィールド | 更新時の状態欠落・不正値を拒否する。作成時の`status`、ユーザーID、その他未知のJSONフィールドも`400 Bad Request`として拒否する | [`rejectsUnknownCreateFields`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerInputValidationTests.java#L151)、[`rejectsMissingOrInvalidStatusAndUnknownFields`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerUpdateValidationTests.java#L59) | ✅ |
| Problem Detailsの入力検証エラー | `application/problem+json`と`status: 400`を返し、検証対象の`field`と日本語`message`を`errors`拡張に含める。項目を特定できないJSON構文エラーでは項目別`errors`を要求しない | [`validatesTitleAndAcceptsTrimmedMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerInputValidationTests.java#L66)、[`returnsProblemDetailsWithoutFieldErrorsForMalformedJson`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerInputValidationTests.java#L173) | ✅ |
| Todo単体の応答DTO | Todo応答に合意済み各フィールドとUTCの`Z`日時を含め、Entityの所有者情報を公開しない | 作成は[`createsTodoAndReturnsLocationAndResponseDto`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerCreateTests.java#L60)、取得・更新は[`getsTodoForOwnerAndReturnsResponseDto`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerReadUpdateTests.java#L59)、[`updatesTodoForOwnerAndReturnsResponseDto`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerReadUpdateTests.java#L83)で確認 | ✅ |
| 一覧ページ応答DTO | 一覧応答は`items`、1始まりの`page`、`size`、`totalElements`、`totalPages`を返し、itemsはTodo応答の形にする | [`returnsDefaultTodoPageForOwner`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerListTests.java#L60)、[`convertsRequestedPageToZeroBasedServicePage`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerListTests.java#L90) | ✅ |

### タスク5: TodoControllerのTDD

`@WebMvcTest`と`MockMvc`でHTTP境界を検証し、`TodoService`と所有者ID解決境界はモックする。DBの検索・永続化は対象外とし、RepositoryおよびServiceテストの既存責務と重ねない。DTOの入力検証、Problem Detailsの入力エラー、応答DTOの形はタスク4のケースをこのControllerテストで実装し、ここでは再掲しない。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 一覧の既定ページとService連携 | クエリ省略時は公開ページ1・サイズ5を使い、0始まりの`Pageable`と解決済み所有者IDをServiceへ渡す。ページ指定時は1始まりを適切に変換し、Serviceの結果を一覧応答にする | [`returnsDefaultTodoPageForOwner`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerListTests.java#L60)、[`convertsRequestedPageToZeroBasedServicePage`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerListTests.java#L90) | ✅ |
| ページクエリの境界 | `page < 1`、`size < 1`、`size > 100`、数値でない値は`400 Bad Request`とし、Serviceを呼ばない。データ範囲を超える有効なページは空の内容と実際のページ情報を持つ`200 OK`とする | [`rejectsInvalidPageAndSizeParameters`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerListBoundaryTests.java#L56)、[`returnsEmptyPageBeyondLastPageWithActualTotals`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerListBoundaryTests.java#L87) | ✅ |
| Todo作成のHTTP契約とService連携 | 有効な要求で`201 Created`と作成Todoへの`Location`を返し、解決済み所有者IDおよびDTOの入力値をServiceへ渡す | [`createsTodoAndReturnsLocationAndResponseDto`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerCreateTests.java#L60) | ✅ |
| Todo取得のHTTP契約とService連携 | 成功時に`200 OK`とTodo応答DTOを返し、解決済み所有者IDとTodo IDでServiceを呼び出す | [`getsTodoForOwnerAndReturnsResponseDto`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerReadUpdateTests.java#L59) | ✅ |
| Todo更新のHTTP契約とService連携 | 有効な要求で`200 OK`と更新後のTodo応答DTOを返し、解決済み所有者ID、Todo ID、タイトル・説明・状態をServiceへ渡す | [`updatesTodoForOwnerAndReturnsResponseDto`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerReadUpdateTests.java#L83) | ✅ |
| Todo削除のHTTP契約とService連携 | 成功時にServiceへ削除を委譲し、本文なしの`204 No Content`を返す | [`deletesTodoForOwnerAndReturnsNoContent`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerDeleteTests.java#L50) | ✅ |
| Todo未検出のHTTPエラー変換 | 取得・更新・削除でServiceが`TodoNotFoundException`を送出した場合、`errors`拡張を付けずProblem Details形式の`404 Not Found`を返す | [`returnsProblemDetailsWhenTodoCannotBeFoundOnRead`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerNotFoundTests.java#L58)、[`returnsProblemDetailsWhenTodoCannotBeFoundOnUpdate`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerNotFoundTests.java#L71)、[`returnsProblemDetailsWhenTodoCannotBeFoundOnDelete`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerNotFoundTests.java#L88) | ✅ |

### タスク6: ローカル所有者境界と初期データ

認証導入前の学習用機能として、ローカル専用プロファイルでのみサンプルデータを初期化する。初期化は起動ごとに専用デモユーザーのTodoを15件へ戻すため、同ユーザーに手動で追加したTodoも再起動時に置き換わる。RepositoryやDB起動を伴うテストは避け、初期化の委譲と所有者IDのHTTP境界をモックで確認した。Section 5では認証済みユーザーによるTodo利用へ置き換え、このローカル所有者機能と専用テストを撤去する（[認証・認可設計仕様](authentication-design.md)）。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 初回起動時のサンプルデータ | デモユーザーが未登録の場合に作成し、そのユーザーに15件のサンプルTodoを初期化する。初期化処理の所有者条件と作成件数をService単体テストで確認する | [`createsOwnerAndFifteenTodosOnFirstInitialization`](../src/test/java/com/github/ok200ok/api_todo/service/LocalSampleDataInitializerTests.java#L46) | ✅ |
| 起動時の再初期化 | 既存デモユーザーのTodoをそのユーザーに限定して置き換え、15件へ戻す。別ユーザーのTodoを対象にしないことをService単体テストで確認する | [`replacesOnlyExistingOwnerTodosOnReinitialization`](../src/test/java/com/github/ok200ok/api_todo/service/LocalSampleDataInitializerTests.java#L73) | ✅ |
| 設定ユーザー名から解決した所有者IDの引き渡し | 設定されたユーザー名から検索されたUserのDB上のUUIDを所有者ID解決境界が返し、ControllerがTodoServiceへ渡すことを確認する | Resolverは[`resolvesDatabaseOwnerIdByConfiguredUsername`](../src/test/java/com/github/ok200ok/api_todo/controller/LocalOwnerIdResolverTests.java#L36)、実ResolverからControllerを通したServiceへの引き渡しは[`passesResolvedOwnerIdToService`](../src/test/java/com/github/ok200ok/api_todo/controller/LocalOwnerIdResolverControllerTests.java#L54)で確認 | ✅ |

### タスク7: OpenAPI／Swagger UI

既存のAPI契約を変更せず、springdoc-openapiがControllerとDTOから生成する仕様を確認する。自動テストは`@WebMvcTest`と`MockMvc`でOpenAPI文書とSwagger UIへのHTTPアクセスを検証し、ブラウザではUI上のエンドポイントとスキーマを目視確認する。Service・Repository・DBの振る舞いは対象外とする。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| OpenAPI仕様の生成 | `/v3/api-docs`がJSONを返し、Todoの一覧・作成・取得・更新・削除のパスとHTTPメソッド、ページクエリ、既存契約の400・404応答を記載する | [`publishesTodoApiOperationsSchemasAndErrorResponses`](../src/test/java/com/github/ok200ok/api_todo/controller/OpenApiDocumentationTests.java#L66) | ✅ |
| スキーマとデータ隠蔽 | リクエスト・応答DTOの項目が仕様に反映され、Entityや所有者情報を公開しない | [`publishesTodoApiOperationsSchemasAndErrorResponses`](../src/test/java/com/github/ok200ok/api_todo/controller/OpenApiDocumentationTests.java#L66) | ✅ |
| Swagger UI | UIがHTTPアクセス可能で、生成されたAPI仕様を表示する。ブラウザでTodoのエンドポイントとスキーマを確認する | UIへのHTTPアクセスは[`redirectsSwaggerUiEntryToUiPage`](../src/test/java/com/github/ok200ok/api_todo/controller/OpenApiDocumentationTests.java#L164)、ブラウザでエンドポイントとスキーマを確認済み | ✅ |

### Section 5 タスク: ユーザー登録とパスワードハッシュ化を担うUserService

`UserService`はユーザー名重複をRepositoryで確認し、平文パスワードを`PasswordEncoder`でハッシュ化してから一般ユーザーとして保存する。HTTP入力の形式・長さ検証は後続のControllerテストで確認し、DB一意制約は既存のUserRepositoryテストに委ねる。このタスクではMockitoを使うService単体テストで、Serviceの判断と依存先への委譲を確認する。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 新規ユーザー登録 | 登録前にユーザー名の重複を確認する。重複がなければ入力パスワードをハッシュ化し、ハッシュ値・ユーザー名・表示名・固定role `ROLE_USER`を設定したUserをRepositoryへ保存して、保存結果を返す | [`registersUserWithEncodedPasswordAndUserRole`](../src/test/java/com/github/ok200ok/api_todo/service/UserServiceTests.java#L43) | ✅ |
| 登録済みユーザー名 | Repositoryの重複確認で既存ユーザーが見つかった場合は登録用例外を送出し、PasswordEncoderによるハッシュ化とUser保存を行わない | [`throwsUserAlreadyExistsExceptionWithoutEncodingOrSaving`](../src/test/java/com/github/ok200ok/api_todo/service/UserServiceTests.java#L68) | ✅ |
| 表示名なしの登録 | `displayName`が`null`でも登録でき、パスワードをハッシュ化したUserをRepositoryへ保存して保存結果を返す | [`registersUserWithoutDisplayName`](../src/test/java/com/github/ok200ok/api_todo/service/UserServiceTests.java#L83) | ✅ |

### Section 5 タスク: JWT発行・検証

JWTの発行にはSpring Securityの`JwtEncoder`、検証には`JwtDecoder`を使い、独自の署名・token解析処理を実装しない。テストでは実際の標準エンコーダー／デコーダーを直接構成し、Springコンテキスト・DBを起動せずにJWTの暗号学的な振る舞いを確認する。BearerヘッダーのHTTP処理とSecurityContextへの認証設定は後続タスクで扱う。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| JWT発行とclaim | JWTを発行し、標準デコーダーで検証できる。`sub`がUser UUID、`iat`が発行時刻、`exp`が発行時刻から15分後である | [`issuesSignedJwtWithUserSubjectAndFifteenMinuteLifetime`](../src/test/java/com/github/ok200ok/api_todo/security/JwtTokenProviderTests.java#L48) | ✅ |
| 不正署名の拒否 | 別の署名鍵で生成されたJWTを標準デコーダーが拒否する | [`rejectsJwtSignedWithDifferentKey`](../src/test/java/com/github/ok200ok/api_todo/security/JwtTokenProviderTests.java#L63) | ✅ |
| 期限切れtokenの拒否 | 有効期限が過去のJWTを標準デコーダーが拒否する | [`rejectsExpiredJwt`](../src/test/java/com/github/ok200ok/api_todo/security/JwtTokenProviderTests.java#L77) | ✅ |

### Section 5 タスク: Spring Security・所有者ID連携

Spring Security Resource ServerのBearer認証を使い、Todo APIで認証済みJWTの`sub`からUser UUIDを解決する。SecurityFilterChainによるHTTP認証を含むためMVCテストで検証し、JWTの署名・期限検証自体は前タスクの実JWTテストを再利用する。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 認証なしのTodo APIアクセス | Bearer tokenなしの保護対象API要求は`401 Unauthorized`となり、TodoServiceを呼び出さない | [`rejectsTodoRequestWithoutBearerToken`](../src/test/java/com/github/ok200ok/api_todo/security/SecurityAuthenticationTests.java#L67) | ✅ |
| 有効なBearer tokenと所有者ID連携 | 認証済みJWTの`sub`にあるUser UUIDを所有者IDとしてControllerからTodoServiceへ渡す | [`passesJwtSubjectAsOwnerIdToTodoService`](../src/test/java/com/github/ok200ok/api_todo/security/SecurityAuthenticationTests.java#L78) | ✅ |
| 不正・期限切れJWT | 不正署名または期限切れのBearer tokenは`401 Unauthorized`となり、TodoServiceを呼び出さない | [`rejectsInvalidAndExpiredBearerTokens`](../src/test/java/com/github/ok200ok/api_todo/security/SecurityAuthenticationTests.java#L97) | ✅ |
| ステートレスな認証設定 | 認証をHTTP sessionへ保存せず、SecurityContextをリクエスト間でsession経由に保持しない | [`doesNotReuseAuthenticationFromHttpSession`](../src/test/java/com/github/ok200ok/api_todo/security/SecurityAuthenticationTests.java#L120) | ✅ |

### Section 5 タスク: JWT本番設定・Controllerテストの認証前提移行

本番用`JwtEncoder`／`JwtDecoder` Beanと署名鍵設定を追加して`@SpringBootTest`とCIを成功させ、既存Controllerテストの所有者前提を`@MockitoBean OwnerIdResolver`から、`SecurityConfiguration`と共有テスト設定`TestJwtConfiguration`を取り込んで実際のBearer tokenを送る認証済み前提へ移行する。ステートレス構成では`spring-security-test`の`jwt()`がSecurityContextへ反映されないため採用しない。Controllerと`OwnerIdResolver`（`AuthenticatedUserIdResolver`）はこの段階では維持し、抽象の削除は撤去タスクで行う。振る舞いを変えない移行のため新規の振る舞いケースは追加せず、既存のControllerケースを引き続き検証する。テスト名・行番号が変わった場合は本一覧のリンクを同期する。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 本番JWT Beanと全コンテキスト起動 | 署名鍵設定から`JwtEncoder`／`JwtDecoder`が提供され、`@SpringBootTest`のコンテキストが起動する。CIの全体テストも成功する | [`contextLoads`](../src/test/java/com/github/ok200ok/api_todo/ApiTodoApplicationTests.java#L23) | ✅ |
| 既存Controllerテストの認証前提化 | 認証済みJWTの`sub`を所有者UUIDとしてServiceへ渡す前提で、既存のHTTP契約・Service連携ケースが引き続き成功する | [`getsTodoForOwnerAndReturnsResponseDto`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerReadUpdateTests.java#L59) ほか既存Controllerテスト | ✅ |

### Section 5 タスク: 登録・ログインAPIとOpenAPI

`AuthController`のHTTP境界を`@WebMvcTest(AuthController.class)`で検証し、`AuthService`はMockito単体テスト、`JpaUserDetailsService`は`UserRepository`をモックした単体テストで確認する。認証APIはBearer tokenなしで利用でき、ログイン失敗は`BadCredentialsException`のみをProblem Detailsの`401`へ変換する。ログインにも登録と同じ`userName`・`password`の入力制約を適用する。OpenAPIは生成された`/v3/api-docs`で認証API、スキーマ、`bearerAuth`を確認する。テストコード作成後に各テスト名へソースリンクを追加する。

現在地（2026-10-20）: 登録API 6ケース、`JpaUserDetailsService` 2ケース、`AuthService` 4ケース、`expiresIn` 1ケース、既存Todoテストのスコープ調整、`AuthenticationManagerConfiguration`追加、ログインController 3ケース、OpenAPI 2ケースまで完了。Section 5タスク6は完了。詳細な引き継ぎは[Section 5タスク6引き継ぎメモ](contexts/section5-task6.md)を参照する。

#### `JpaUserDetailsServiceTests`

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 登録済みユーザーの読み込み | `userName`、保存済みパスワードハッシュ、`ROLE_USER`権限を持つ`UserDetails`を返す。`UserRepository.findByUserName`にユーザー名を渡す | [`loadsUserDetailsWithStoredPasswordAndRole`](../src/test/java/com/github/ok200ok/api_todo/security/JpaUserDetailsServiceTests.java#L39) | ✅ |
| 存在しないユーザー名 | `UsernameNotFoundException`を送出する | [`throwsUsernameNotFoundExceptionForUnknownUserName`](../src/test/java/com/github/ok200ok/api_todo/security/JpaUserDetailsServiceTests.java#L58) | ✅ |

#### `AuthServiceTests`

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 登録とtoken発行 | `UserService.registerUser`へユーザー名・平文パスワード・表示名を渡す。返されたUserのUUIDでtokenを発行し、User・token・`expiresIn`（900）を返す | [`registersUserAndIssuesTokenForNewUser`](../src/test/java/com/github/ok200ok/api_todo/service/AuthServiceTests.java#L57) | ✅ |
| 重複ユーザー名 | `UserAlreadyExistsException`をそのまま伝播し、tokenを発行しない | [`propagatesDuplicateUserNameWithoutIssuingToken`](../src/test/java/com/github/ok200ok/api_todo/service/AuthServiceTests.java#L79) | ✅ |
| ログイン成功 | `AuthenticationManager`へ未認証の`UsernamePasswordAuthenticationToken`を渡す。認証成功後、`Authentication.getName()`でUserを再検索し、UUIDでtokenを発行する | [`authenticatesCredentialsAndIssuesTokenForUserUuid`](../src/test/java/com/github/ok200ok/api_todo/service/AuthServiceTests.java#L94) | ✅ |
| ログイン失敗 | `BadCredentialsException`をそのまま伝播し、User再検索とtoken発行を行わない | [`propagatesAuthenticationFailureWithoutIssuingToken`](../src/test/java/com/github/ok200ok/api_todo/service/AuthServiceTests.java#L123) | ✅ |

#### `AuthControllerRegisterTests`

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 登録成功 | `201 Created`、`application/json`、`userName`・`displayName`・`accessToken`・`tokenType`（`Bearer`）・`expiresIn`（900）を返す。`Location`、`id`、`password`、`role`は返さない。`AuthService`へ入力を渡す | [`registersUserAndReturnsCreatedWithToken`](../src/test/java/com/github/ok200ok/api_todo/controller/AuthControllerRegisterTests.java#L52) | ✅ |
| 表示名なし | `displayName`省略で`201`、応答の`displayName`は`null`。`AuthService`へ`null`を渡す | [`registersUserWithoutDisplayName`](../src/test/java/com/github/ok200ok/api_todo/controller/AuthControllerRegisterTests.java#L80) | ✅ |
| 重複ユーザー名 | `409 Conflict`、`application/problem+json`、`status: 409`、`detail: 指定されたユーザー名は既に登録されています`。`errors`拡張は付けない | [`returnsConflictWhenUserNameAlreadyExists`](../src/test/java/com/github/ok200ok/api_todo/controller/AuthControllerRegisterTests.java#L99) | ✅ |
| 入力検証 | `userName`の欠落・空白のみ・大文字・記号・51文字、`password`の欠落・7文字・UTF-8で73バイト以上、`displayName`の51文字を`400`のProblem Detailsで拒否し、`detail: 入力内容を確認してください`と該当`field`を`errors`に含める。`AuthService`を呼ばない | [`rejectsInvalidRegistrationInput`](../src/test/java/com/github/ok200ok/api_todo/controller/AuthControllerRegisterTests.java#L121) | ✅ |
| パスワードの72バイト境界 | `"あ".repeat(24)`（72バイト）を受理して`201`。`AuthService`へ同じ文字列を渡す | [`acceptsPasswordAtUtf8ByteLimit`](../src/test/java/com/github/ok200ok/api_todo/controller/AuthControllerRegisterTests.java#L158) | ✅ |
| 未知フィールド・不正JSON | 未知フィールドとJSON構文エラーを`400`のProblem Detailsで拒否する。`detail: リクエスト本文を読み取れません`。項目を特定できないため`errors`は付けない。`AuthService`を呼ばない | [`rejectsUnknownFieldsAndMalformedJsonOnRegistration`](../src/test/java/com/github/ok200ok/api_todo/controller/AuthControllerRegisterTests.java#L178) | ✅ |

#### `AuthControllerLoginTests`

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| ログイン成功 | `200 OK`、`application/json`、`TokenResponse`として`accessToken`・`tokenType`（`Bearer`）・`expiresIn`（900）を返す。ユーザー情報、`id`、`password`、`role`は返さない。`AuthService.login(userName, password)`へ入力を渡す | [`logsInAndReturnsToken`](../src/test/java/com/github/ok200ok/api_todo/controller/AuthControllerLoginTests.java#L50) | ✅ |
| 資格情報不一致 | `401 Unauthorized`、`application/problem+json`、`status: 401`、`detail: ユーザー名またはパスワードが正しくありません`。`errors`拡張と`WWW-Authenticate`ヘッダーを付けない。`BadCredentialsException`のみを変換する | [`returnsUnauthorizedWhenCredentialsAreInvalid`](../src/test/java/com/github/ok200ok/api_todo/controller/AuthControllerLoginTests.java#L75) | ✅ |
| 入力検証 | 登録と同じ`userName`・`password`制約（登録の`rejectsInvalidRegistrationInput`から`displayName`を除く）で`400`とし、`detail: 入力内容を確認してください`と該当`field`を`errors`に含める。`AuthService`を呼ばない | [`rejectsInvalidLoginInput`](../src/test/java/com/github/ok200ok/api_todo/controller/AuthControllerLoginTests.java#L98) | ✅ |

#### `OpenApiDocumentationTests`

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 認証APIとスキーマ | `/api/v1/auth/register`の`post`（`201`・`400`・`409`）と`/api/v1/auth/login`の`post`（`200`・`400`・`401`）が文書化される。`RegisterRequest`・`LoginRequest`・`RegisterResponse`・`TokenResponse`のスキーマが生成され、応答に`password`・`role`・`id`を含まない。`OpenApiConfiguration`を`@Import`して検証する | [`publishesAuthApiOperationsAndSchemas`](../src/test/java/com/github/ok200ok/api_todo/controller/OpenApiDocumentationTests.java#L107) | ✅ |
| Bearerスキームと適用範囲 | `components.securitySchemes.bearerAuth`が`type: http`、`scheme: bearer`、`bearerFormat: JWT`を持つ。Todo操作には`bearerAuth`の`security`が付き、認証APIには付かない。`OpenApiConfiguration`を`@Import`して検証する | [`definesBearerSecuritySchemeAndAppliesItToTodoOperations`](../src/test/java/com/github/ok200ok/api_todo/controller/OpenApiDocumentationTests.java#L144) | ✅ |

#### `JwtTokenProviderTests`

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| `expiresIn`の契約 | 既存のJWT発行テストに、`getExpiresInSeconds()`が`900`を返す確認を追加する | [`issuesSignedJwtWithUserSubjectAndFifteenMinuteLifetime`](../src/test/java/com/github/ok200ok/api_todo/security/JwtTokenProviderTests.java#L57)に追加 | ✅ |

既存のTodo系・セキュリティ系10クラスは`AuthController`追加の影響を避けるため、`@WebMvcTest(TodoController.class)`へ限定済み。`OpenApiDocumentationTests`は`@MockitoBean AuthService`と`@Import(OpenApiConfiguration.class)`を追加済み。いずれも振る舞いは変えない。

| Section / タスク | 対象レイヤーコンポーネント・テスト範囲 | 状態 |
| --- | --- | --- |
| Section 5: 認証方式とAPI契約 | [認証・認可設計仕様](authentication-design.md)で登録・ログイン、パスワードハッシュ、JWT、認可境界、エラー応答を合意する | ✅ |
| Section 5: ユーザー登録とパスワードハッシュ化を担うUserService | 下記ケースをMockitoによるService単体テストで検証する | ✅ |
| Section 5: JWT発行・検証 | `JwtEncoder`と`JwtDecoder`の標準実装を使い、Springコンテキスト・DBなしのテストで署名付きJWTの発行と検証を確認する | ✅ |
| Section 5: Spring Security・所有者ID連携 | 下記4ケースでBearer認証、認証主体からの所有者ID解決、ステートレス構成を検証する | ✅ |
| Section 5: JWT本番設定・Controllerテストの認証前提移行 | 上記2ケースで本番JWT Beanの起動と認証前提のControllerテスト移行を確認する | ✅ |
| Section 5: 登録・ログインAPIとOpenAPI | 登録API・ログインAPI・Service/UserDetails連携・既存Controllerテストのスコープ調整・OpenAPI 2ケースまで完了 | ✅ |
| Section 5: ローカル機能撤去 | `LocalOwnerIdResolver`・`LocalSampleDataInitializer`・`application-local.yaml`と専用テスト、および不要になった`OwnerIdResolver`抽象を撤去し、Controllerが認証主体から所有者を解決する形へ整理する | 🚧 |
| Section 6: 認証接続後の結合テスト・総仕上げ | 認証からTodo操作までのフローと全テスト検証範囲を合意後に記録する | 🚧 |
