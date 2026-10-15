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

## Section 3以降

未合意の後続タスクは、仕様とテストケースを開発者と合意した時点で具体的な観点・期待結果・テストリンクを追記する。ケース内容を先回りして固定しない。

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
| 所有者別一覧の取得 | Serviceが所有者IDをRepositoryへ渡し、返された一覧を返す | `findsTodosForOwner`（作成予定） | 🤝 |
| 該当Todoがない一覧 | Repositoryが空の結果を返す場合、Serviceも空リストを返す | `returnsEmptyListWhenOwnerHasNoTodos`（作成予定） | 🤝 |
| Repositoryの所有者条件と順序 | PostgreSQL上で指定所有者のTodoのみを返し、`updated_at`降順、同時刻ではID降順になる | `findsTodosByOwnerOrderedByUpdatedAtAndIdDescending`（作成予定） | 🤝 |

| Section / タスク | 対象レイヤーコンポーネント・テスト範囲 | 状態 |
| --- | --- | --- |
| Section 3: Mockitoを用いた単体テスト手法 | Serviceテストの依存置換と検証境界を理解する | ✅ |
| Section 3: TodoService作成 | 合意した所有者付き作成ケースをService単体テストで確認する | ✅ |
| Section 3: TodoService ID指定取得 | 合意した所有者条件付き検索と不存在のケースをService単体・Repository実DBテストで確認する | ✅ |
| Section 3: TodoService所有者別一覧 | 合意した所有者境界、空リスト、更新日時・ID順をService単体・Repository実DBテストで確認する | 🤝 |
| Section 3: TodoService更新 | 所有者条件を含む更新と対象なしの扱いを合意後にケースを記録する | 🚧 |
| Section 3: TodoService削除 | 所有者条件を含む削除と対象なしの扱いを合意後にケースを記録する | 🚧 |
| Section 4: Todo API契約・DTO・Validation | URL、HTTP、入出力、エラー、入力制約を合意後にケースを記録する。ユーザーIDはリクエストに含めず、他ユーザー所有または不存在のTodoは404とする | 🚧 |
| Section 4: TodoController | `@WebMvcTest`でHTTP境界とService連携を検証するケースを合意後に記録する | 🚧 |
| Section 4: ローカル所有者境界 | API動作確認用ユーザーとControllerからServiceへ所有者IDを渡す境界を確認する | 🚧 |
| Section 4: OpenAPI / Swagger UI | 生成文書とブラウザでの確認内容を合意後に記録する | 🚧 |
| Section 5: 認証追加前の結合テスト | ローカル所有者境界を含むAPIフローとTestcontainersの範囲を合意後に記録する | 🚧 |
| Section 6: UserService登録・パスワードハッシュ化 | 登録仕様とハッシュ化の検証範囲を合意後にケースを記録する | 🚧 |
| Section 6: Spring Security / JWT | 認証・認可の責務、トークン仕様、テスト範囲を合意後に記録する | 🚧 |
| Section 6: ログインAPI・AuthenticationFilter | 認証フローとTodo APIへの認証済み所有者連携を合意後に記録する | 🚧 |
| Section 6: 認証要件のAPI契約・OpenAPI反映 | 未認証時の応答と認証要件の追加ケースを合意後に記録する | 🚧 |
| Section 7: 認証接続後の結合テスト・総仕上げ | 認証からTodo操作までのフローと全テスト検証範囲を合意後に記録する | 🚧 |
