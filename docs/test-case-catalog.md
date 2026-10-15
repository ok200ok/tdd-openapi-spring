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
| 🤝合意済み | 仕様、ケース、期待結果に合意したが、対象テストコードや実装内容の確認・理解は未実施|
| 👓読解済み・実行待ち | 対象テストコードと実装の意図を確認・理解したが、開発者によるテスト実行は未実施 |
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

Serviceは所有者条件付きでTodoを取得し、タイトル・説明・状態をひとまとまりで変更して更新後のTodoを返す。Repositoryの所有者条件は前タスクの実DBテストで確認済みのため重複せず、トランザクションによるdirty checkingの実動作はSection 5の結合テストで扱う。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 所有者本人のTodo更新 | タイトル・説明・状態のすべてが変更され、更新後のTodoが返る | [`updatesTodoForOwner`](../src/test/java/com/github/ok200ok/api_todo/service/TodoServiceTests.java#L166) | ✅ |
| 不存在・別所有者のTodo更新 | 所有者条件付き検索が空の場合は`TodoNotFoundException`となり、Entityの変更操作は行われない | [`throwsTodoNotFoundExceptionWhenUpdatingTodoNotOwnedByUser`](../src/test/java/com/github/ok200ok/api_todo/service/TodoServiceTests.java#L188) | ✅ |

### タスク: Todo削除

Serviceは所有者条件付きでTodoを取得してから削除する。不存在と所有者不一致は同じ`TodoNotFoundException`として扱い、他ユーザーのTodoを削除しない。Repositoryの所有者条件は前タスクの実DBテストを再利用する。Service単体テストでは削除メソッドの呼び出しを確認し、JPAによる実DB削除はSection 5の結合テストで確認する。

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
| 作成・更新DTOの正常入力 | 作成DTOはタイトルと任意説明を受け付け、状態・ユーザーIDを持たない。更新DTOはタイトル・説明・状態を受け付ける。タイトル前後の空白は除去される | [`validatesTitleAndAcceptsTrimmedMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerInputValidationTests.java#L53)、[`trimsAndValidatesTitleOnUpdate`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerUpdateValidationTests.java#L60) | 🤝 |
| タイトルの検証 | 欠落・`null`・空白のみを拒否し、trim後101文字を拒否する。trim後100文字は受け入れる | [`validatesTitleAndAcceptsTrimmedMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerInputValidationTests.java#L53)、[`trimsAndValidatesTitleOnUpdate`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerUpdateValidationTests.java#L60) | 🤝 |
| 説明の省略・null | 作成時は説明省略を許容する。PUTでは説明の省略または`null`を既存説明の削除として扱う。説明長はこのAPIでは制限しない | [`acceptsOptionalNullAndLongDescriptionOnCreate`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerInputValidationTests.java#L97)、[`treatsOmittedAndNullDescriptionAsRemovalAndAllowsLongDescription`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerUpdateValidationTests.java#L91) | 🤝 |
| 状態と未知JSONフィールド | 更新時の状態欠落・不正値を拒否する。作成時の`status`、ユーザーID、その他未知のJSONフィールドも`400 Bad Request`として拒否する | [`rejectsUnknownCreateFields`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerInputValidationTests.java#L134)、[`rejectsMissingOrInvalidStatusAndUnknownFields`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerUpdateValidationTests.java#L44) | 🤝 |
| Problem Detailsの入力検証エラー | `application/problem+json`と`status: 400`を返し、検証対象の`field`と日本語`message`を`errors`拡張に含める。項目を特定できないJSON構文エラーでは項目別`errors`を要求しない | [`validatesTitleAndAcceptsTrimmedMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerInputValidationTests.java#L53)、[`returnsProblemDetailsWithoutFieldErrorsForMalformedJson`](../src/test/java/com/github/ok200ok/api_todo/controller/TodoControllerInputValidationTests.java#L154) | ✅ |
| 応答DTOの契約 | Todo応答に合意済み各フィールドとUTCの`Z`日時を含める。一覧応答は`items`、1始まりの`page`、`size`、`totalElements`、`totalPages`を返す | Controllerテスト作成後にリンクを追加 | 🤝 |

### タスク5: TodoControllerのTDD

`@WebMvcTest`と`MockMvc`でHTTP境界を検証し、`TodoService`と所有者ID解決境界はモックする。DBの検索・永続化は対象外とし、RepositoryおよびServiceテストの既存責務と重ねない。DTOの入力検証、Problem Detailsの入力エラー、応答DTOの形はタスク4のケースをこのControllerテストで実装し、ここでは再掲しない。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 一覧の既定ページとService連携 | クエリ省略時は公開ページ1・サイズ5を使い、0始まりの`Pageable`と解決済み所有者IDをServiceへ渡す。ページ指定時は1始まりを適切に変換し、Serviceの結果を一覧応答にする | Controllerテスト作成後にリンクを追加 | 🤝 |
| ページクエリの境界 | `page < 1`、`size < 1`、`size > 100`、数値でない値は`400 Bad Request`とし、Serviceを呼ばない。データ範囲を超える有効なページは空の内容と実際のページ情報を持つ`200 OK`とする | Controllerテスト作成後にリンクを追加 | 🤝 |
| Todo作成のHTTP契約とService連携 | 有効な要求で`201 Created`と作成Todoへの`Location`を返し、解決済み所有者IDおよびDTOの入力値をServiceへ渡す | Controllerテスト作成後にリンクを追加 | 🤝 |
| Todo取得のHTTP契約とService連携 | 成功時に`200 OK`を返し、解決済み所有者IDとTodo IDでServiceを呼び出す | Controllerテスト作成後にリンクを追加 | 🤝 |
| Todo更新のHTTP契約とService連携 | 有効な要求で`200 OK`を返し、解決済み所有者ID、Todo ID、タイトル・説明・状態をServiceへ渡す | Controllerテスト作成後にリンクを追加 | 🤝 |
| Todo削除のHTTP契約とService連携 | 成功時にServiceへ削除を委譲し、本文なしの`204 No Content`を返す | Controllerテスト作成後にリンクを追加 | 🤝 |
| Todo未検出のHTTPエラー変換 | 取得・更新・削除でServiceが`TodoNotFoundException`を送出した場合、`errors`拡張を付けずProblem Details形式の`404 Not Found`を返す | Controllerテスト作成後にリンクを追加 | 🤝 |

| Section / タスク | 対象レイヤーコンポーネント・テスト範囲 | 状態 |
| --- | --- | --- |
| Section 4: UTC日時への見直し | UserとTodoの作成・更新日時の`timestamptz`永続化・再読込と更新時刻の振る舞いをRepositoryテストで確認する | ✅ |
| Section 4: Todo一覧ページネーション | 所有者条件付きDBページ分割、順序・ページ情報・範囲外ページ、およびServiceのRepository委譲を確認する | ✅ |
| Section 4: DTO・Validation | DTOの入力制約、未知JSON項目の拒否、入力エラーProblem Details、応答DTOのフィールドと日時・ページ表現をタスク5のControllerテストで一度だけ確認する | 🤝 |
| Section 4: TodoController | `@WebMvcTest`でCRUDのHTTP契約、Service連携、ページクエリ境界、未検出例外の404変換を確認する。DTO・Validationのケースは上記タスク4の記録を参照する | 🤝 |
| Section 4: ローカル所有者境界・シードデータ | ローカル専用ユーザーとTodo約15件の初期データ、ControllerからServiceへ所有者IDを渡す境界を確認する | 🚧 |
| Section 4: OpenAPI / Swagger UI | 生成文書とブラウザでの確認内容を合意後に記録する | 🚧 |
| Section 5: 認証追加前の結合テスト | ローカル所有者境界を含むAPIフローとTestcontainersの範囲を合意後に記録する | 🚧 |
| Section 6: UserService登録・パスワードハッシュ化 | 登録仕様とハッシュ化の検証範囲を合意後にケースを記録する | 🚧 |
| Section 6: Spring Security / JWT | 認証・認可の責務、トークン仕様、テスト範囲を合意後に記録する | 🚧 |
| Section 6: ログインAPI・AuthenticationFilter | 認証フローとTodo APIへの認証済み所有者連携を合意後に記録する | 🚧 |
| Section 6: 認証要件のAPI契約・OpenAPI反映 | 未認証時の応答と認証要件の追加ケースを合意後に記録する | 🚧 |
| Section 7: 認証接続後の結合テスト・総仕上げ | 認証からTodo操作までのフローと全テスト検証範囲を合意後に記録する | 🚧 |
