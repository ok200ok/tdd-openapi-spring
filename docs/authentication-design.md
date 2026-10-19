# 認証・認可設計仕様

Section 5の認証・認可について、確定した目的、API契約、実装構成、テスト境界をまとめる。個々のテストケース、期待結果、リンク、実行状態は[テストケース一覧](test-case-catalog.md)で管理する。現在地と次の作業は[Section 5タスク6引き継ぎメモ](contexts/section5-task6.md)を参照する。

## 目的と範囲

- ユーザー登録時にパスワードをハッシュ化して保存し、ログイン時に資格情報を検証する
- JWT Bearer access tokenで認証し、Todo APIでは認証済みユーザーのIDを所有者として使う
- Todoの所有者境界を維持し、ユーザーIDをクライアント入力から受け取らない
- Spring Securityの標準認証機能を優先し、独自のJWT検証フィルターなどを作らない
- Section 6で認証からTodo操作までの結合テスト、全テスト、最終レビューと文書整理を行う

### 対象外

- Refresh token、トークン失効リスト、ログアウトによる発行済みトークンの無効化
- OAuth 2.0/OIDCの外部IdP連携、多要素認証、メール確認、パスワードリセット
- ユーザー削除とTodoの連鎖削除
- 本番運用向けの鍵ローテーション基盤やレート制限。本番導入時には別途設計が必要

## 既存ドメインとの整合

- Userの`userName`は一意で、小文字英数字のみ、最大50文字。認証APIでも既存のデータモデル制約を守る
- `displayName`は任意・最大50文字で、ログイン識別子には使わない
- Userの`password`列には平文でなく、PasswordEncoderが生成したハッシュを保存する。パスワードやハッシュを応答・ログへ出さない
- 新規登録のroleはサーバー側で一般ユーザーに固定し、クライアントに指定させない
- Todo APIの所有者は認証済み主体から解決する。不存在と他ユーザー所有のTodoは既存契約どおり同じ`404 Not Found`とする

## 実装構成

| コンポーネント | 役割 |
| --- | --- |
| `JpaUserDetailsService` | `UserRepository`からUserを取得し、保存済みパスワードハッシュとroleをSpring Securityの`UserDetails`へ変換する |
| `AuthenticationManagerConfiguration` | `DaoAuthenticationProvider`に`JpaUserDetailsService`と`PasswordEncoder`を設定し、`ProviderManager`を`AuthenticationManager`として公開する |
| `PasswordEncoderConfiguration` | `BCryptPasswordEncoder`をBeanとして提供する |
| `JwtConfiguration` | `app.security.jwt.secret`から`JwtEncoder`／`JwtDecoder`を生成する |
| `JwtTokenProvider` | User UUIDを`sub`にした15分有効のHS256署名JWTを発行し、`expiresIn`を秒で返す |
| `SecurityConfiguration` | 認証APIを公開し、Todo APIをBearer JWT必須のステートレス構成にする |
| `AuthService` | 登録・ログインのユースケースを調整し、保存済みUserのUUIDでtokenを発行する |
| `AuthController` | 登録・ログインのHTTP入力をDTOで受け取り、`AuthService`へ委譲して応答DTOへ変換する |

## 認証フロー

1. 登録要求を検証し、`UserService`がユーザー名の重複確認、パスワードのハッシュ化、一般ユーザーとしての保存を行う
2. 登録成功後、保存済みUserのUUIDを`sub`にしたJWT access tokenを発行する
3. ログイン要求では`AuthenticationManager`へ資格情報の検証を委譲する
4. `DaoAuthenticationProvider`が`JpaUserDetailsService`でUserを取得し、`PasswordEncoder`で保存済みハッシュと平文パスワードを照合する
5. 認証成功後、認証済みユーザー名でUserを再取得し、UUIDを`sub`にしたJWTを発行する
6. Todo APIへのBearer tokenはSpring Security OAuth2 Resource Serverの標準機能で検証する
7. 認証済み主体のUUIDを`OwnerIdResolver`経由でTodoServiceへ所有者IDとして渡す

ユーザー不存在とパスワード不一致は区別せず、`BadCredentialsException`として扱う。`DaoAuthenticationProvider`の`hideUserNotFoundExceptions`を有効にし、アカウントの存在を推測させない。

## API契約

| 操作 | URL・メソッド | 入力 | 成功応答 |
| --- | --- | --- | --- |
| ユーザー登録 | `POST /api/v1/auth/register` | `userName`, `password`, 任意の`displayName` | `201 Created` |
| ログイン | `POST /api/v1/auth/login` | `userName`, `password` | `200 OK` |

### 登録応答

`201 Created`で`application/json`の`RegisterResponse`を返す。`Location`ヘッダーは付けない。

```json
{
  "userName": "alice",
  "displayName": "Alice",
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

- `displayName`は未指定なら`null`
- `id`、`password`、`role`は返さない
- `tokenType`は固定で`Bearer`、`expiresIn`は900秒

### ログイン応答

`200 OK`で`application/json`の`TokenResponse`を返す。

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

- ユーザー名、表示名、`id`、`password`、`role`は返さない
- `tokenType`は固定で`Bearer`、`expiresIn`は900秒

ログインにも登録と同じ`userName`・`password`の入力制約を適用する。`userName`は必須・小文字英数字のみ・最大50文字、`password`は必須・8文字以上・UTF-8で72バイト以下とする。

## エラー応答

認証APIのエラーはRFC Problem Details形式（`application/problem+json`）で返す。

| 状況 | status | detail | errors拡張 | 補足 |
| --- | --- | --- | --- | --- |
| 入力制約違反 | `400 Bad Request` | `入力内容を確認してください` | `field`と日本語`message`を含む | 登録・ログイン共通 |
| 未知フィールド・JSON構文エラー | `400 Bad Request` | `リクエスト本文を読み取れません` | 付けない | 項目を特定できないため |
| 登録済み`userName` | `409 Conflict` | `指定されたユーザー名は既に登録されています` | 付けない | `UserAlreadyExistsException` |
| ログイン資格情報の不一致 | `401 Unauthorized` | `ユーザー名またはパスワードが正しくありません` | 付けない | `BadCredentialsException`のみ変換 |

ログインの`401`には`WWW-Authenticate`ヘッダーを付けない。ログインAPIはBearer tokenで保護されたリソースではなく、クライアントへ認証方式を問い合わせるチャレンジを返す必要がないため、Problem Detailsで理由を伝える。

Todo APIへの未認証・不正・期限切れtokenはResource Serverが`401 Unauthorized`を返す。認証済みだが必要な権限がない場合は`403 Forbidden`とする。現時点では一般ユーザーのみを想定し、管理者機能は対象外とする。

## JWT・パスワード方針

| 項目 | 内容 |
| --- | --- |
| access token | JWT Bearer tokenのみ。refresh tokenなし |
| subject (`sub`) | UserのUUID |
| 有効期限 | 15分（`expiresIn`は900秒） |
| 署名方式 | HS256（HMAC署名） |
| 署名鍵 | `app.security.jwt.secret`。環境変数`JWT_SECRET`で上書きできる |
| パスワードハッシュ | `BCryptPasswordEncoder` |
| パスワード長 | 8文字以上、UTF-8で72バイト以下 |

`JwtConfiguration`は設定文字列からHS256用の鍵を生成し、`JwtEncoder`／`JwtDecoder`を提供する。実暗号を扱うテストはテスト内の独立鍵を使い、アプリ既定値に依存しない。`@SpringBootTest`はアプリ既定鍵で起動する。

パスワードのUTF-8バイト長は独自制約`@Utf8ByteLength`で検証する。Javaの文字数ではなく実際のバイト長を比較し、BCryptの入力上限による切り詰めを避ける。`null`は`@NotBlank`など必須チェックの責務に委ね、この制約では違反にしない。

### Subject (`sub`)にUser UUIDを使う理由

JWTの`sub`は、そのtokenが誰についてのものかを表す標準claim。User UUIDはユーザー名の変更や表示名と無関係に安定しており、DB上のUserを一意に特定できる。ユーザー名をclaimにすると変更可能な識別子がtokenに固定されるうえ、ログなどにユーザー名が現れる機会も増える。UUIDは秘密情報ではなく、tokenの署名検証と有効期限によってtokenの真正性・有効性を確認する。

## OpenAPI

- `OpenApiConfiguration`で`bearerAuth`を定義する
  - `type: http`
  - `scheme: bearer`
  - `bearerFormat: JWT`
- `TodoController`にクラスレベルの`@SecurityRequirement(name = "bearerAuth")`を付け、Todo操作へBearer認証を要求する
- `AuthController`の登録・ログインには`@SecurityRequirement`を付けない
- AuthControllerの操作には、登録`201`・`400`・`409`、ログイン`200`・`400`・`401`の応答を明示する
- `OpenApiDocumentationTests`は`OpenApiConfiguration`を取り込み、`/v3/api-docs`でauth paths、DTOスキーマ、`bearerAuth`、Todo操作の`security`を確認する

## テスト境界

- `JpaUserDetailsServiceTests`: `UserRepository`をMockitoで置き換え、`UserDetails`への変換だけを確認する
- `AuthServiceTests`: `AuthenticationManager`などをMockitoで置き換え、Serviceの委譲とtoken発行を確認する
- `AuthControllerRegisterTests`／`AuthControllerLoginTests`: `@WebMvcTest(AuthController.class)`、`SecurityConfiguration`、`TestJwtConfiguration`、`@MockitoBean AuthService`でHTTP境界を確認する
- `OpenApiDocumentationTests`: 全Controllerを読み込み、`TodoService`と`AuthService`をモックしてOpenAPI文書とSwagger UIを確認する
- `ApiTodoApplicationTests`: `@SpringBootTest`とTestcontainersで全コンテキストとPostgreSQL接続を確認する
- 実際の`DaoAuthenticationProvider`を通したログインHTTPフローはSection 6の結合テストで確認する

## ローカルデモ機能の移行・撤去

登録・ログインAPI完成後、認証前の仮ユーザー機能を撤去する。`OwnerIdResolver`抽象は、認証用とローカル用の2実装が存在する間は必要であり、ローカル撤去と同時に不要になるため、この撤去タスクでまとめて削除する。

- `LocalOwnerIdResolver`、`LocalSampleDataInitializer`、`application-local.yaml`の所有者設定、専用デモデータを対象にしたテストを削除する
- `application-local.yaml`が他用途にも使われていないか確認し、ファイル削除または不要設定だけの削除を選ぶ
- ローカル専用ユーザー名`localdemo`が通常登録される可能性、および固定パスワード文字列の扱いを確認する
- 不要になった`OwnerIdResolver`抽象と`AuthenticatedUserIdResolver`を削除し、Controllerが`@AuthenticationPrincipal Jwt`から所有者UUIDを解決する形へ整理する
- ControllerのHTTPテストは削除せず、認証済みUserを使うテストのまま維持する
- Section 6の結合テストでは、テスト内でユーザーを登録・認証し、そのユーザーのtokenでTodo APIを操作する
