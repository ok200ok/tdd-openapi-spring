# 認証・認可設計仕様

認証・認可の仕組み、コンポーネント責務、セキュリティ上の判断をまとめる。HTTP契約とエラー応答は[API設計仕様](api-design.md)、DTOと入力検証は[DTO・Validation設計仕様](dto-validation.md)、User/Todoのデータ制約は[データモデル設計仕様](data-modeling.md)、テストケースと実行状態は[テストケース一覧](test-case-catalog.md)を正とする。

## 目的と範囲

- 登録時にパスワードをハッシュ化して保存し、ログイン時に資格情報を検証する
- JWT Bearer access tokenで認証し、Todo APIの所有者を認証済みユーザーIDから解決する
- ユーザーIDをクライアント入力から受け取らず、所有者本人だけがTodoを操作できるようにする
- Spring Securityの標準機能を使い、独自のJWT検証フィルターを作らない

### 対象外

- Refresh token、トークン失効リスト、ログアウトによる発行済みtokenの無効化
- 外部IdP連携、多要素認証、メール確認、パスワードリセット
- ユーザー削除とTodoの連鎖削除
- 本番運用向けの鍵ローテーション、レート制限

## コンポーネントと責務

| コンポーネント | 責務 |
| --- | --- |
| `JpaUserDetailsService` | `UserRepository`のUserを、保存済みハッシュとroleを持つ`UserDetails`へ変換する |
| `AuthenticationManagerConfiguration` | `DaoAuthenticationProvider`へ`JpaUserDetailsService`と`PasswordEncoder`を設定し、`AuthenticationManager`を公開する |
| `PasswordEncoderConfiguration` | `BCryptPasswordEncoder`をBeanとして提供する |
| `JwtConfiguration` | `app.security.jwt.secret`から`JwtEncoder`／`JwtDecoder`を生成する |
| `JwtTokenProvider` | User UUIDを`sub`にした15分有効のHS256署名JWTを発行する |
| `SecurityConfiguration` | 認証APIを公開し、Todo APIをBearer JWT必須のステートレス構成にする |
| `AuthService` | 登録・ログインを調整し、保存済みUserのUUIDでtokenを発行する |
| `AuthController` | 認証APIのHTTP入出力をDTOへ変換し、`AuthService`へ委譲する |

## 認証フロー

### 登録

1. `AuthController`が入力を検証し、`AuthService`へ渡す
2. `UserService`がユーザー名の重複確認、パスワードのハッシュ化、一般ユーザーとしての保存を行う
3. `JwtTokenProvider`が保存済みUserのUUIDを`sub`にしたaccess tokenを発行する

### ログイン

1. `AuthService`が`AuthenticationManager`へ資格情報の検証を委譲する
2. `DaoAuthenticationProvider`が`JpaUserDetailsService`でUserを取得し、`PasswordEncoder`で保存済みハッシュと照合する
3. 認証成功後、認証済みユーザー名でUserを再取得し、UUIDを`sub`にしたaccess tokenを発行する

### Todo API利用

1. Resource ServerがBearer tokenの署名と有効期限を検証する
2. `TodoController`が認証済みJWTの`sub`から所有者UUIDを解決する
3. `TodoService`が所有者条件付きでTodoを操作する

## 認可境界

- `SecurityConfiguration`は認証APIを`permitAll`、Todo APIを`authenticated`とし、Swagger UIなどその他の要求は許可する
- セッションを使わないステートレス構成とし、要求ごとにBearer tokenを検証する
- Bearer認証のみを使うためCSRF保護は無効化する
- Todoの所有者はJWTの`sub`だけから決め、リクエストのユーザーIDは受け取らない
- 認証済みでも他ユーザーのTodoは存在を明かさず`404 Not Found`とし、所有者不一致に`403`を使わない
- tokenなし・不正・期限切れはResource Serverが`401 Unauthorized`を返す。`403`は認証済みだが権限が不足する場合に限る（現状の管理者機能は対象外）

## JWT・パスワード方針

| 項目 | 方針 |
| --- | --- |
| access token | JWT Bearer tokenのみ。refresh tokenなし |
| subject (`sub`) | UserのUUID |
| 有効期限 | 15分（`expiresIn`は900秒） |
| 署名方式 | HS256 |
| 署名鍵 | `app.security.jwt.secret`。環境変数`JWT_SECRET`で上書きする |
| パスワードハッシュ | `BCryptPasswordEncoder` |
| パスワード長 | 入力制約は[DTO・Validation設計仕様](dto-validation.md)を参照する |

`sub`にUUIDを使うのは、ユーザー名や表示名の変更に影響されずDB上のUserを一意に特定できるため。ユーザー名をclaimへ固定せず、ログに識別子が現れる機会も抑える。

## 失敗時のセキュリティ判断

- ユーザー不存在とパスワード不一致は区別せず`BadCredentialsException`とし、`hideUserNotFoundExceptions`でアカウントの存在を推測させない
- ログイン失敗は`401 Unauthorized`のProblem Detailsで返し、`WWW-Authenticate`は付けない。ログインAPIはBearerで保護されたリソースではないため
- 認証APIの入力検証・未知フィールド・重複ユーザー名の応答は[API設計仕様](api-design.md)と[DTO・Validation設計仕様](dto-validation.md)に従う

## テスト境界

| 層 | テスト手段 | 確認範囲 |
| --- | --- | --- |
| 認証コンポーネント・Service | Mockito単体テスト | 資格情報の委譲、token発行、`UserDetails`変換 |
| HTTP境界 | `@WebMvcTest`・`MockMvc` | SecurityFilterChain、Bearer認証、入力検証、Problem Details |
| 結合 | `@SpringBootTest(RANDOM_PORT)`・Testcontainers | 実HTTPサーバー、PostgreSQL、本番JWTによる登録・ログイン・Todo CRUD・所有者分離 |

個別のテストケースと実行状態は[テストケース一覧](test-case-catalog.md)で管理する。

## ローカルデモ機能の撤去

認証導入前の固定所有者・サンプルデータ機能は撤去済み（履歴は[テストケース一覧](test-case-catalog.md)の撤去済みケースを参照）。
