# テストケース一覧

この一覧は、各タスクで検討・合意したテストケースと期待結果を集約する学習資料。テストの有無だけでなく、「何の仕様を、どのレイヤーコンポーネントで、なぜ検証するのか」を後から追えるようにする。

## 記載・更新ルール

- タスクのテストケースを開発者と合意した時点で、実装や解説に進む前に一覧へ記載する。
- 各テストケース表の状態欄には、凡例に定義した対応する絵文字だけを記載する。説明文は凡例に置き、状態欄へ文字を併記しない。
- 新規テストの追加・修正時はテスト名、期待結果、リンクを同期する。既存ケースと異なる仕様を検証する場合はケースを分ける。
- テストメソッド名のリンクはソースファイルと行番号へ結び付ける。コード変更で行番号や名前が変わった場合はリンクを更新する。
- 各タスクの完了前に、合意済みケースの記載漏れ、期待結果、リンク、状態を確認する。
- 後続タスクの具体的なケースは仕様合意時に決める。未合意のテスト内容を先回りして仕様として固定しない。

## 状態

| 状態 | 意味 |
| --- | --- |
| 🚧未検討 | 対象タスクでケースをまだ協議していない |
| 🤝合意済み・読解待ち | 既存ケースと期待結果に合意し、解説・確認が残っている |
| 👓読解済み・実行待ち | ケースと実装の意図を確認し、開発者による実行確認が残っている |
| ✅実行確認済み | 開発者が実行し、結果を共有して期待結果との一致を確認した |

## Section 2: JPA / Repository

このSectionは先行実装済みテストの振り返りであり、新規TDDサイクルとして扱わない。テスト実行状態は開発者が結果を共有した場合にのみ更新する。

### タスク2: Entity・RepositoryとPostgreSQL Testcontainers

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| Repository経由のUser保存とID検索、生成値、DB接続先 | 保存時にID・作成/更新時刻が設定され、指定したUser情報を取得できる。`SELECT version()`でPostgreSQL 17への接続を確認する。検索は同一永続化コンテキスト内で行うためDBからの再読込は保証しない | [`savesAndReloadsUserWithGeneratedUuidAndTimestamps`](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java#L49) | ✅ |

### タスク3: Userの基本マッピング

上限ちょうどの値を許すことなど、記載のない境界値まではこの合意に含めない。

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| UUIDと作成時タイムスタンプ | 新規保存時にUUID、`createdAt`、`updatedAt`が設定される | [`savesAndReloadsUserWithGeneratedUuidAndTimestamps`](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java#L49) | ✅ |
| 必須の`userName` | `userName`がないUserの保存がNOT NULL制約により拒否され、Springのデータアクセス例外として通知される | [`rejectsMissingUsername`](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java#L104) | ✅ |
| 必須の`password` | `password`がないUserの保存がNOT NULL制約により拒否され、Springのデータアクセス例外として通知される | [`rejectsMissingPassword`](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java#L115) | ✅ |
| 必須の`role` | `role`がないUserの保存がNOT NULL制約により拒否され、Springのデータアクセス例外として通知される | [`rejectsMissingRole`](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java#L126) | ✅ |
| 任意の`displayName` | `displayName`がnullでも保存でき、取得後もnullである | [`allowsDisplayNameToBeNull`](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java#L185) | ✅ |
| `userName`の最大長超過 | 51文字のユーザー名を保存するとDB制約違反になる | [`rejectsUsernameExceedingMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java#L138) | ✅ |
| `password`の最大長超過 | 256文字のパスワード文字列を保存するとDB制約違反になる | [`rejectsPasswordExceedingMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java#L150) | ✅ |
| `displayName`の最大長超過 | 51文字の表示名を保存するとDB制約違反になる | [`rejectsDisplayNameExceedingMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java#L162) | ✅ |
| `role`の最大長超過 | 51文字のRoleを保存するとDB制約違反になる | [`rejectsRoleExceedingMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java#L173) | ✅ |
| 更新時の`updatedAt` | 表示名変更をflushすると、更新時刻が変更前より後になる | [`updatesUpdatedAtWhenUserChanges`](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java#L199) | ✅ |

### タスク4: `userName`の正規化・形式・一意性・検索

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 小文字への正規化と検索 | 大文字を含むユーザー名を保存するとEntityが小文字へ正規化し、小文字の名前で検索できる | [`canonicalizesUsernameAndFindsItByCanonicalName`](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java#L73) | ✅ |
| 正規化後の一意性 | 大文字小文字だけが異なる2件目を保存すると正規化後の名前が重複し、DBの一意制約で拒否される | [`rejectsDuplicateCanonicalUsernames`](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java#L100) | ✅ |
| 許容文字 | 小文字英数字以外を含む名前はEntity正規化だけでは除去されず、DBのCHECK制約で拒否される | [`rejectsUsernamesContainingNonAlphanumericCharacters`](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java#L113) | ✅ |
| 表示名の重複許可 | 異なるユーザー名のUser 2件に同じ`displayName`を設定しても、両方保存できる | [`allowsDuplicateDisplayNames`](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java#L87) | ✅ |

### タスク5: Todoの基本マッピング

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 採番IDと作成時刻 | Todo保存時にDB採番IDと作成/更新時刻が設定される。Ownerの関連ID検証はタスク6で扱う | [`savesTodoWithGeneratedIdAndRequiredUser`](../src/test/java/com/github/ok200ok/api_todo/TodoRepositoryTests.java#L51) | ✅ |
| 各フィールドとEnum文字列保存 | Todoのタイトル、任意の説明、状態を保存・再取得でき、各Enum名を文字列としてDBに保存する | [`reloadsAllTodoFieldsAndEnumAsItsStringValue`](../src/test/java/com/github/ok200ok/api_todo/TodoRepositoryTests.java#L70)、[`persistsAllStatusesByTheirEnumNames`](../src/test/java/com/github/ok200ok/api_todo/TodoRepositoryTests.java#L122) | ✅ |
| 長い説明文 | `description`に長文を保存できる | [`allowsLongDescriptionText`](../src/test/java/com/github/ok200ok/api_todo/TodoRepositoryTests.java#L105) | ✅ |
| 更新時の`updatedAt` | 状態変更をflushすると、更新時刻が変更前より後になる | [`updatesUpdatedAtWhenTodoChanges`](../src/test/java/com/github/ok200ok/api_todo/TodoRepositoryTests.java#L139) | ✅ |
| 必須のタイトルと状態 | `title`または`status`を欠くTodoの保存がDB制約違反になる | [`rejectsMissingTodoTitle`](../src/test/java/com/github/ok200ok/api_todo/TodoRepositoryTests.java#L165)、[`rejectsMissingTodoStatus`](../src/test/java/com/github/ok200ok/api_todo/TodoRepositoryTests.java#L178) | ✅ |
| タイトルの最大長 | 101文字のタイトルを保存するとDB制約違反になる | [`rejectsTitleExceedingMaximumLength`](../src/test/java/com/github/ok200ok/api_todo/TodoRepositoryTests.java#L192) | ✅ |

### タスク6: TodoとUserのリレーション

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| 必須Ownerと関連ID | 保存前にUserを永続化し、Todoの関連がそのUser IDを参照することを確認する。OwnerがnullのTodoはDB制約で拒否される。所有者別検索や認可はこのRepositoryテストでは検証しない | [`savesTodoWithGeneratedIdAndRequiredUser`](../src/test/java/com/github/ok200ok/api_todo/TodoRepositoryTests.java#L52)、[`rejectsMissingTodoOwner`](../src/test/java/com/github/ok200ok/api_todo/TodoRepositoryTests.java#L160) | ✅ |
| 遅延ロード | DBから再取得した直後はTodoのuser関連が未ロードであり、userNameへのアクセス時に関連を利用できる | [`keepsUserAssociationLazyUntilAccessed`](../src/test/java/com/github/ok200ok/api_todo/TodoRepositoryTests.java#L93) | ✅ |
| 外部キーと削除制約 | todos.user_idの外部キーが存在し、参照中Userの削除がDB制約で拒否される。テストはTodo行数の事後確認までは行わない | [`doesNotDeleteTodosWhenTheirUserIsDeleted`](../src/test/java/com/github/ok200ok/api_todo/TodoRepositoryTests.java#L222) | ✅ |

### タスク7: Section 2全体のテストと設計照合

| 観点 | 期待する結果・確認目的 | テスト | 状態 |
| --- | --- | --- | --- |
| Section 2のRepositoryテストと既存結合テスト | `.\gradlew.bat test`で全27件（UserRepositoryTests 14件、TodoRepositoryTests 11件、ApiTodoApplicationTests 2件）が失敗・エラーなく成功する。Repositoryテストの結果をデータモデル文書と各ケースの期待結果に照らし、カタログ・READMEの記載と一致することを確認する | [UserRepositoryTests](../src/test/java/com/github/ok200ok/api_todo/UserRepositoryTests.java)、[TodoRepositoryTests](../src/test/java/com/github/ok200ok/api_todo/TodoRepositoryTests.java)、[ApiTodoApplicationTests](../src/test/java/com/github/ok200ok/api_todo/ApiTodoApplicationTests.java) | ✅ |

## Section 3以降

以下のタスクは、仕様とテストケースを開発者と合意した時点で具体的な観点・期待結果・テストリンクを追記する。ケース内容は未合意のため、ここではテスト仕様を先取りしない。

| Section / タスク | 対象レイヤーコンポーネント・テスト範囲 | 状態 |
| --- | --- | --- |
| Section 3: Mockitoを用いた単体テスト手法 | Serviceテストの依存置換、テスト範囲、検証方法を協議する | 🚧 |
| Section 3: UserService | ユースケース仕様合意後にケースを記録する | 🚧 |
| Section 3: TodoService CRUD | CRUD仕様・所有者認可などを合意後にケースを記録する | 🚧 |
| Section 4: Spring Security / JWTの仕組み | 認証・認可の責務とテスト範囲を協議する | 🚧 |
| Section 4: JWT発行・検証 | トークン仕様合意後にケースを記録する | 🚧 |
| Section 4: ログインAPI・AuthenticationFilter | 認証フロー合意後にケースを記録する | 🚧 |
| Section 5: API契約・DTO・Validation | URL、HTTP、応答、エラー、入力制約を合意後にケースを記録する | 🚧 |
| Section 5: User/Auth Controller | Controller契約合意後にケースを記録する | 🚧 |
| Section 5: Todo Controller | 認証を含むAPI契約合意後にケースを記録する | 🚧 |
| Section 5: OpenAPI / Swagger UI | 生成文書とブラウザでの確認内容を合意後に記録する | 🚧 |
| Section 6: 結合テスト | 対象フローとTestcontainersの検証範囲を合意後にケースを記録する | 🚧 |
