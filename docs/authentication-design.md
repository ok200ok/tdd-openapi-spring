# 認証・認可設計仕様

Section 5で扱う認証・認可の目的、API契約、セキュリティ方針、テスト範囲を合意・更新する文書。合意前の案は「要合意」と明記し、確定した仕様は実装とテストケース一覧へ反映する。

## 目的と範囲

- ユーザー登録時にパスワードをハッシュ化して保存し、ログイン時に資格情報を検証する。
- JWT Bearer access tokenで認証し、Todo APIでは認証済みユーザーのIDを所有者として使う。
- Todoの所有者境界を維持し、ユーザーIDをクライアント入力から受け取らない。
- Spring Securityの標準認証機能を優先し、独自のJWT検証フィルターなどを作らない。
- Section 6で認証からTodo操作までの結合テスト、全テスト、最終レビューと文書整理を行う。

### 対象外

- Refresh token、トークン失効リスト、ログアウトによる発行済みトークンの無効化
- OAuth 2.0/OIDCの外部IdP連携、多要素認証、メール確認、パスワードリセット
- ユーザー削除とTodoの連鎖削除
- 本番運用向けの鍵ローテーション基盤やレート制限。本番導入時には別途設計が必要

## 既存ドメインとの整合

- Userの`userName`は一意で、小文字英数字のみ、最大50文字。認証APIでも既存のデータモデル制約を守る。
- `displayName`は任意・最大50文字で、ログイン識別子には使わない。
- Userの`password`列には平文でなく、PasswordEncoderが生成したハッシュを保存する。パスワードやハッシュを応答・ログへ出さない。
- 新規登録のroleはサーバー側で一般ユーザーに固定し、クライアントに指定させない。
- Todo APIの所有者は、認証済み主体から解決する。不存在と他ユーザー所有のTodoは既存契約どおり同じ`404 Not Found`とする。

## 認証フロー案

1. 登録要求を検証し、既存UserRepositoryで重複しないことを確認する。
2. UserServiceがPasswordEncoderでパスワードをハッシュ化し、一般ユーザーとしてUserを保存する。
3. ログイン要求ではAuthenticationManager等のSpring Security標準認証部品で資格情報を検証する。
4. 認証成功時に署名付きJWT access tokenを返す。
5. Todo APIへのBearer tokenはSpring Security OAuth2 Resource Serverの標準機能で検証する。
6. 認証主体からUserのUUIDを取り出し、TodoServiceへ所有者IDとして渡す。旧LocalOwnerIdResolverは置き換える。

JWT発行にはSpring Securityの`JwtEncoder`（Nimbus実装）、検証には`JwtDecoder`（Nimbus実装）を使う。Resource ServerはHTTPの`Authorization: Bearer`ヘッダーからtokenを取り出し、設定された`JwtDecoder`で署名・有効期間などを検証したうえで認証済み主体をSecurityContextへ設定する。これらを組み合わせることで、独自のBearer token解析フィルターやJWT署名検証コードを避ける。

`JwtEncoder`はJWT claimを署名付きtokenへ変換する役割、`JwtDecoder`は受け取ったtokenを検証してclaimへ戻す役割を持つ。発行専用の薄いクラスが必要なら`JwtEncoder`へclaimを渡す責務だけを持たせ、署名やtoken文字列の解析はSpring Securityへ委譲する。具体的な依存関係・バージョンと設定APIは実装前に公式資料を確認する。

## 合意済みAPI契約

| 操作 | URL・メソッド | 入力 | 成功応答 |
| --- | --- | --- | --- |
| ユーザー登録 | `POST /api/v1/auth/register` | `userName`, `password`, 任意の`displayName` | `201 Created`。ユーザー公開情報とBearer access tokenを返し、登録完了と同時にログイン状態にする |
| ログイン | `POST /api/v1/auth/login` | `userName`, `password` | `200 OK`。`accessToken`, `tokenType`, `expiresIn`を返す |

登録・ログインは共通のtoken応答項目（`accessToken`, `tokenType`, `expiresIn`秒）を使う。登録応答にはさらにユーザー公開情報（`userName`、`displayName`）を含め、パスワードとroleは含めない。アカウント保存後、通常のログインと同じ署名・有効期限の規則でtokenを発行する。ユーザーのUUIDはtokenのsubjectと所有者識別に使うが、登録応答で別途返す必要はない。

### エラー応答案

- 入力制約違反: 既存APIと同じProblem Details形式の`400 Bad Request`。
- 登録済み`userName`: `409 Conflict`。重複の有無が分かる応答はユーザー列挙につながり得るが、公開登録APIの一般的なUXを優先する。機密性の高い登録では別途見直す。
- ログイン資格情報の不一致: ユーザー不存在とパスワード不一致を区別しない`401 Unauthorized`。
- Todo APIへの未認証・不正・期限切れtoken: `401 Unauthorized`。
- 認証済みだが必要な権限がない場合: `403 Forbidden`。現時点では一般ユーザーのみを想定し、管理者機能は対象外。

認証APIのProblem Details詳細はテストケース合意時に確定する。

## 合意済みJWT・パスワード方針

| 項目 | 合意内容 | 補足 |
| --- | --- | --- |
| access token | JWT Bearer tokenのみ。refresh tokenなし | 再ログイン頻度と有効期限 |
| subject (`sub`) | UserのUUID | tokenの`sub`から認証済みUserを特定する |
| 有効期限 | 15分 | 漏えいしたtokenを利用できる時間を短くする |
| 署名方式 | 単一アプリ内で扱いやすいHMAC署名 | 複数サービス化・鍵共有の予定があれば非対称署名を再検討 |
| 署名鍵 | 環境設定から注入し、ソースに秘密値を置かない | ローカル設定、テスト用鍵、本番のsecret管理を分離する |
| パスワードハッシュ | Spring Securityの`BCryptPasswordEncoder` | 適切なwork factorを使い、入力を72 UTF-8 bytes以下に制限する |
| パスワード長 | 8文字以上、UTF-8で72 bytes以下 | BCryptの入力上限による切り詰めを避ける。Unicode文字は複数bytesになり得る |

JWTの署名・検証にはSpring Securityの標準機能を用い、具体的なエンコード・デコード部品と依存関係を確認して確定する。秘密鍵に弱い既定値を設けて本番利用可能に見せる設定は避ける。

### 署名鍵の設定（合意）

本プロジェクトはデプロイせず、実行環境はローカル開発とCIのみとする。これを踏まえ、署名鍵は次のように扱う。

- アプリ設定`app.security.jwt.secret`に環境変数`JWT_SECRET`で上書きできる既定値を持たせる。既定値は「ローカル学習専用」と分かる十分長い値（HS256要件の256ビット以上）にし、日本語コメントで本番では環境変数で上書きする旨を明記する。
- 実暗号を扱うテスト（`JwtTokenProviderTests`／`SecurityAuthenticationTests`）はテスト内の独立鍵を使い、アプリ既定値に依存しない。
- `@SpringBootTest`はアプリ既定鍵で起動し、CIの全体テストを成功させる。
- 将来デプロイする場合は既定値を外し、環境変数を必須にして本番用secret管理へ分離する。

`JwtEncoder`／`JwtDecoder`は`security`パッケージの設定クラスで、上記秘密鍵から生成する。正確なSpring Security／NimbusのAPIは実装直前に公式資料で確認する。

### Subject (`sub`)にUser UUIDを使う理由

JWTの`sub`（subject）は、そのtokenが誰についてのものかを表す標準claim。User UUIDはユーザー名の変更や表示名と無関係に安定しており、DB上のUserを一意に特定できる。ユーザー名をclaimにすると変更可能な識別子がtokenに固定されるうえ、ログなどにユーザー名が現れる機会も増える。UUIDは秘密情報ではなく、tokenの署名検証と有効期限によってtokenの真正性・有効性を確認する。

登録成功時にtokenを発行すること自体は避ける必要はない。メール確認を導入していない本システムでは、登録後すぐに利用できる一貫したUXになる。一方、後からメール確認を必須にするなら、確認完了前にTodo API用tokenを発行しない方針へ変える。公開登録では自動化された大量登録を防ぐレート制限等が本番運用で必要になるが、これは現スコープ外である。

## 本番JWT設定とControllerテストの認証前提移行

`SecurityConfiguration`がResource Serverの標準フィルターを組み立てるには`JwtDecoder`が必須だが、現状はテスト内の`@TestConfiguration`にしかない。本番用に次の設定を追加する。

- `app.security.jwt.secret`から`JwtEncoder`／`JwtDecoder`を生成する`security`パッケージの設定クラスを追加し、`@SpringBootTest`とCIを成功させる。
- 既存Controllerテストの所有者前提を、`@MockitoBean OwnerIdResolver`と未認証要求から、`SecurityConfiguration`と共有テスト設定`TestJwtConfiguration`を取り込み、`JwtTokenProvider`で発行した実Bearer tokenを送る認証済み要求へ移行する。ステートレス構成では`spring-security-test`の`jwt()`が`SecurityContextHolder`へ反映されないため採用しない。
- この段階ではControllerと`OwnerIdResolver`（`AuthenticatedUserIdResolver`）を維持し、抽象の削除は撤去タスクで行う。

## ローカルデモ機能の移行・撤去

認証済みユーザー登録でTodo APIを利用できるようになった時点、すなわち登録・ログインAPI完成後に、認証前の仮ユーザー機能を最後のタスクとして撤去する。`OwnerIdResolver`抽象は、認証用とローカル用の2実装が存在する間は必要であり、ローカル撤去と同時に不要になるため、この撤去タスクでまとめて削除する。

- `LocalOwnerIdResolver`、`LocalSampleDataInitializer`、`application-local.yaml`の所有者設定、専用デモデータを対象にしたテストを削除する。
- `application-local.yaml`が他用途にも使われていないか確認し、ファイル削除または不要設定だけの削除を選ぶ。
- ローカル専用ユーザー名`localdemo`が通常登録される可能性、および固定パスワード文字列の扱いを確認する。
- 不要になった`OwnerIdResolver`抽象と`AuthenticatedUserIdResolver`を削除し、Controllerが認証主体（`@AuthenticationPrincipal Jwt`）から所有者UUIDを解決する形へ整理する。
- ControllerのHTTPテストは削除せず、認証済みUserを使うテストのまま維持する。
- Section 6の結合テストでは、テスト内でユーザーを登録・認証し、そのユーザーのtokenでTodo APIを操作する。

## 実装・テストの順序案

1. 認証方式、認証API契約、JWT、パスワード制約、認可応答を合意する（完了）。
2. UserServiceの登録・重複・ハッシュ化をMockito単体テストでTDDする（完了）。
3. JWTの生成・検証をSpringコンテキストやDBを起動しないテストで確認する（完了）。
4. Spring SecurityのBearer認証と認証主体からのUser ID解決を、MVCテストで2ケースずつに分けてTDDする（完了）。
5. 本番用`JwtEncoder`／`JwtDecoder` Beanと署名鍵設定を追加し、`@SpringBootTest`とCIを成功させる。既存Controllerテストを認証前提へ移行する。
6. 登録・ログインControllerを`@WebMvcTest`で検証し、OpenAPIへ認証・登録・ログイン契約を反映する。
7. ローカルデモ機能と、不要になった`OwnerIdResolver`抽象を撤去する。
8. Section 6でTestcontainersを用いた認証からTodo操作までの結合テスト、全テスト実行、文書整合性確認を行う。

### JWTテストでSpringコンテキストとDBを起動しない理由

Springコンテキストは、SpringがBeanの生成・依存関係の注入・設定の適用を行う実行環境。JWT単体テストの目的は、標準エンコーダーとデコーダーによる署名・claim・有効期間の動作確認なので、テスト内でそれらを直接構成すれば十分である。SpringコンテキストやDBを起動しないことで、起動時間と外部環境への依存を避け、失敗箇所をJWTの振る舞いに絞れる。

このテストだけではSpring SecurityのHTTPフィルターチェーン、Bearer tokenの取り出し、認証済み主体のController連携までは保証しない。それらは次のSecurityテストおよびSection 6の結合テストで確認するため、テスト種別ごとに検証境界を分ける。

SecurityのMVCテストではTodoServiceをモックにし、HTTP要求からフィルターチェーン、認証主体、Controllerの所有者ID受け渡しまでを確認する。Bearer filterとControllerの連携を対象にするためHTTPフィルターを有効にした`@WebMvcTest`を使い、DBやJPAの起動は不要とする。テスト用の鍵を使った実際のNimbus `JwtEncoder`／`JwtDecoder`で有効・不正署名・期限切れtokenを作成・検証し、署名検証結果がHTTP応答へ変換されるところまで確認する。

テストケースの期待結果・リンク・実行状態は[テストケース一覧](test-case-catalog.md)で管理する。

## 実装前に確認する事項

- JWT署名方式の具体的なアルゴリズム、鍵形式、Spring Securityの標準エンコード・デコード部品。
- BCryptのwork factorと、テスト・ローカル・本番環境それぞれの署名鍵管理方法。
- `sub`以外にJWTへ含めるclaimとroleの認可利用範囲。
- 登録・ログイン応答DTOの具体的なJava型とProblem Detailsの詳細。
