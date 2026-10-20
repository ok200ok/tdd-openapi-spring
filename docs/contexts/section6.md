# Section 6 引き継ぎメモ

このファイルは、新しいセッションでSection 6「認証接続後の結合テストと総仕上げ」を開始するための現在地・方針・次の操作をまとめたメモ。仕様の正は次の文書とし、このメモではSection 6の開始に必要な判断と作業手順だけを補う。

- [認証・認可設計仕様](../authentication-design.md)
- [DTO・Validation設計仕様](../dto-validation.md)
- [API設計仕様](../api-design.md)
- [テストケース一覧](../test-case-catalog.md)
- [READMEのロードマップ](../../README.md)

## このメモの扱い

- Section 5は完了済み。最新の全体テストは106件成功で、作業ツリーはクリーン。
- Section 6のテストケースはまだ合意・追加していない。現在の106件に新規結合テストは含まれていない。

## 新しいセッション開始時の必須操作

1. `AGENTS.md`、`README.md`、`docs/test-case-catalog.md`、このメモ、`git status`、直近のGit履歴を確認する。
2. 対象タスクをSection 6の1番目「`@SpringBootTest` + `Testcontainers`による認証からTodo操作までの一連の処理を検証する」と宣言する。
3. 確認直後、下記「開始時にそのまま提示する文面」を省略・要約せずに提示する。これがSection 6タスク1の「目的・仕様の合意」段階である。
4. 開発者の合意を待ち、テストケースの合意へ進む。合意前にテストコードや本番コードを作成しない。

## 開始時にそのまま提示する文面

```markdown
## Section 6 タスク1: 目的・仕様の合意

**目的**
実際のHTTPサーバー・PostgreSQL・JWTを通して、ユーザー登録／ログインからTodoの作成・取得・更新・削除までを一連で検証します。タスク6メモに残っている「実際の`DaoAuthenticationProvider`を通したログインHTTPフロー未検証」も、ここで解消します。

**検証方式**
- `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- `@AutoConfigureRestTestClient` + `RestTestClient`
- `@Import(TestcontainersConfiguration.class)`
- 新規クラス: `AuthTodoIntegrationTests`

`MockMvc`ではなく実ポートのHTTPクライアントを使う理由は、SecurityFilterChain、Bearer認証、JSON変換、DBトランザクションを実際の起動サーバー経由で確認するためです。追加依存は不要で、現在の`spring-boot-starter-webmvc-test`に含まれています。

**対象範囲**
- 実DBへのユーザー登録とパスワードハッシュ保存
- 実`AuthenticationManager`によるログインとJWT発行
- 発行されたJWTを使ったTodo CRUD
- 所有者本人だけがTodoを操作できること
- Testcontainers上の独立DBを使用し、開発用DBは変更しない

**対象外**
- 入力検証の全パターン、OpenAPI契約、JWT期限切れ・不正署名の詳細
- これらは既存のController／Security／OpenAPIテストで確認済み

**受け入れ条件**
- 新規結合テストがGREEN
- 既存106件を含む全テストがGREEN
- テストケース一覧と引き継ぎ資料を実態に合わせて更新

この方針で進めてよいですか。特に、**別ユーザーが他人のTodoを取得・更新・削除しようとすると404になる所有者分離**も本タスクに含める想定でよいか確認したいです。
```

## 現在地（2026-10-20）

- ブランチ: `docs/integration-docs`
- HEAD: `27695b0 Merge pull request #17 from ok200ok/feature/springsec-jwtauth`
- 作業ツリー: クリーン
- Section 5: 完了
- Section 6: 未着手。READMEの3項目はすべて未チェック
- 全体テスト: 106件成功（Section 5タスク7完了時点）
- 開発用PostgreSQL: Testcontainersとは分離。データ削除の必要がない限り`docker compose down -v`を実行しない
- 開発DBの`localdemo`ユーザーとTodo15件はSection 5タスク7で削除済み

## Section 5で完了した主な変更

- 認証前の固定所有者機能を撤去
  - `OwnerIdResolver`、`LocalOwnerIdResolver`、`AuthenticatedUserIdResolver`
  - `LocalSampleDataInitializer`、`application-local.yaml`
  - ローカル専用テスト3クラス
- `TodoController`は`@AuthenticationPrincipal Jwt`を受け取り、`jwt.getSubject()`から所有者UUIDを解決
- `@AuthenticationPrincipal Jwt`がOpenAPIのリクエストパラメータとして露出しないよう、Todo各操作で`@Parameter(hidden = true)`を付与
- README、認証設計、API設計、テストケース一覧を更新済み
- 全体106件成功を開発者が確認済み

## Section 6のロードマップ

1. `@SpringBootTest` + `Testcontainers`で認証からTodo操作までの一連の処理を検証する（現在の対象、未着手）
2. `./gradlew test`で全テストスイートを一括実行確認する
3. 最終コードレビューとドキュメント整理を行う

## タスク1の提案（未合意）

### 新規テストクラス

`src/test/java/com/github/ok200ok/api_todo/AuthTodoIntegrationTests.java`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class AuthTodoIntegrationTests {
	@Autowired
	private RestTestClient restTestClient;
}
```

### 採用するAPI

- HTTPクライアント: `org.springframework.test.web.servlet.client.RestTestClient`
- 自動設定: `org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient`
- DB: 既存の`TestcontainersConfiguration`と`postgres:17-alpine`
- JWT: 本番用の`JwtEncoder` / `JwtDecoder`と`app.security.jwt.secret`
- `TestJwtConfiguration`は使わない。固定テスト鍵ではなく実際のアプリ設定で発行・検証する
- `@MockitoBean`、`@WebMvcTest`、`MockMvc`は使わない

### 検証する一連の処理

1. ユーザーAを`POST /api/v1/auth/register`で登録し、201とaccess tokenを受け取る
2. 同じ資格情報で`POST /api/v1/auth/login`を実行し、実際の`DaoAuthenticationProvider`経由で200とaccess tokenを受け取る
3. ログインで得たBearer tokenを使い、`POST /api/v1/todos`でTodoを作成する
4. `GET /api/v1/todos`で一覧取得し、作成したTodoとページ情報を確認する
5. `GET /api/v1/todos/{todoId}`で個別取得する
6. `PUT /api/v1/todos/{todoId}`でタイトル・説明・状態を更新する
7. `DELETE /api/v1/todos/{todoId}`で削除し、204を確認する
8. 削除後の個別取得が404になることを確認する

### 所有者分離の確認候補

- ユーザーAとユーザーBを登録・ログインする
- AがTodoを作成する
- BのtokenでAのTodoを取得・更新・削除し、いずれも404になることを確認する
- Bの一覧にはAのTodoが含まれないことを確認する

### 未認証・ログイン失敗の確認候補

- Bearer tokenなしのTodo操作が401になること
- 誤ったパスワードのログインが401になり、ユーザー存在有無を区別しないこと

上記の候補はまだ合意前。目的・仕様の合意後、テストケースの合意段階で採否と件数を確定し、`docs/test-case-catalog.md`へ記録する。

## テストデータと分離

- 各テストで一意なユーザー名を使い、テスト実行順に依存しない
- ユーザー名は小文字英数字のみ・最大50文字。例: `alice` + UUIDの先頭8文字
- パスワードは8文字以上・UTF-8で72バイト以内。例: `password123`
- `@Transactional`でテストをロールバックしない。HTTPサーバーは別スレッドでDBへコミットするため、ロールバック前提は成立しない
- Testcontainersのコンテナはテストコンテキストごとに独立しているため、原則として明示的なDB削除は不要

## API契約の要点

### 登録

`POST /api/v1/auth/register`

```json
{"userName":"alice","password":"password123","displayName":"Alice"}
```

- 201 Created
- `userName`、`displayName`、`accessToken`、`tokenType`、`expiresIn`
- 409はユーザー名重複、400は入力不正

### ログイン

`POST /api/v1/auth/login`

```json
{"userName":"alice","password":"password123"}
```

- 200 OK
- `accessToken`、`tokenType`、`expiresIn`
- 401は資格情報不一致

### Todo作成

`POST /api/v1/todos`

```json
{"title":"Read documentation","description":"Spring Security"}
```

- 201 Created
- `Location: /api/v1/todos/{todoId}`
- `id`、`title`、`description`、`status`、`createdAt`、`updatedAt`
- 初期状態は`PENDING`

### Todo一覧

`GET /api/v1/todos?page=1&size=5`

- `items`、`page`、`size`、`totalElements`、`totalPages`
- ページ番号は1始まり

### Todo取得・更新・削除

- `GET /api/v1/todos/{todoId}`: 200、他人所有または不存在は404
- `PUT /api/v1/todos/{todoId}`: `title`、`description`、`status`を置き換え、200
- `DELETE /api/v1/todos/{todoId}`: 204、他人所有または不存在は404
- `status`は`PENDING`、`IN_PROGRESS`、`COMPLETED`

## TDDと検証プロトコル

- 今回の結合テストは既存機能の検証であり、新規本番コードを原則伴わない
- テストコード作成後、対象テストが最初からGREENになる可能性がある。GREENは既存実装が結合条件を満たす証拠として扱う
- 失敗した場合は、単純な機能REDとして扱わず、実際のHTTP・認証・DB連携の不整合として原因を分析する
- テストコード作成後は対象クラスだけを実行し、結果を開発者へ共有して停止する
- 開発者のGREEN確認後、必要ならRefactorし、最後に全体テストを実行する
- 新規テスト件数を確定するまで「全体106 + 新規件数」の成功件数を固定しない

## 実行コマンド

```powershell
# 新規結合テスト
.\gradlew.bat test --tests 'com.github.ok200ok.api_todo.AuthTodoIntegrationTests'

# 全体
.\gradlew.bat test
```

HTMLレポート: `build/reports/tests/test/index.html`

## 注意事項

- 開発用DBを結合テストへ使わない。必ず`TestcontainersConfiguration`を`@Import`する
- 開発用DBのデータを削除する場合を除き、`docker compose down -v`を実行しない
- 認証APIはBearer tokenなしで利用でき、Todo APIはBearer token必須という既存契約を変えない
- 所有者不一致は403ではなく404という既存契約を変えない
- OpenAPIのJWT非表示を壊さない。`TodoController`の`@Parameter(hidden = true)`を外さない
- コミットやGit操作の単位は開発者が決める。テストケース合意前にコミットしない
- テスト名・行番号を変更した場合は`docs/test-case-catalog.md`のリンクを同期する

## 主なファイル

- `src/main/java/com/github/ok200ok/api_todo/controller/AuthController.java`
- `src/main/java/com/github/ok200ok/api_todo/controller/TodoController.java`
- `src/main/java/com/github/ok200ok/api_todo/controller/ApiExceptionHandler.java`
- `src/main/java/com/github/ok200ok/api_todo/security/SecurityConfiguration.java`
- `src/main/java/com/github/ok200ok/api_todo/security/JwtTokenProvider.java`
- `src/main/java/com/github/ok200ok/api_todo/config/AuthenticationManagerConfiguration.java`
- `src/main/java/com/github/ok200ok/api_todo/dto/RegisterRequest.java`
- `src/main/java/com/github/ok200ok/api_todo/dto/RegisterResponse.java`
- `src/main/java/com/github/ok200ok/api_todo/dto/LoginRequest.java`
- `src/main/java/com/github/ok200ok/api_todo/dto/TokenResponse.java`
- `src/main/java/com/github/ok200ok/api_todo/dto/CreateTodoRequest.java`
- `src/main/java/com/github/ok200ok/api_todo/dto/UpdateTodoRequest.java`
- `src/main/java/com/github/ok200ok/api_todo/dto/TodoResponse.java`
- `src/main/java/com/github/ok200ok/api_todo/dto/TodoPageResponse.java`
- `src/test/java/com/github/ok200ok/api_todo/TestcontainersConfiguration.java`
- `src/test/java/com/github/ok200ok/api_todo/ApiTodoApplicationTests.java`
- `src/test/java/com/github/ok200ok/api_todo/security/SecurityAuthenticationTests.java`
- `src/test/java/com/github/ok200ok/api_todo/controller/AuthControllerLoginTests.java`
- `src/test/java/com/github/ok200ok/api_todo/controller/OpenApiDocumentationTests.java`
- `docs/test-case-catalog.md`
- `README.md`

## 次の操作

1. 上記「開始時にそのまま提示する文面」を提示して停止する
2. 開発者の合意後、テストケースの観点・期待結果・対象レイヤー・新規件数を合意する
3. 合意内容を`docs/test-case-catalog.md`へ追記する
4. テストコードだけを作成し、対象テストの実行結果を確認して停止する
