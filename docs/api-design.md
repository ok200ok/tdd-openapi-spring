# API設計仕様

HTTP APIの公開契約（パス、HTTPメソッド、ステータス、ヘッダー、ページネーション、エラー形式）を定義する。DTOの項目と入力制約は[DTO・Validation設計仕様](dto-validation.md)、認証・認可方式は[認証・認可設計仕様](authentication-design.md)、永続データの構造は[データモデル設計仕様](data-modeling.md)、テストケースと実行状態は[テストケース一覧](test-case-catalog.md)を正とする。

## 目的と範囲

- Todo APIと認証APIのHTTP契約を定義し、Entityを外部へ公開しない
- リソース指向と一貫したステータス・エラー形式を保ち、クライアントが契約を実装できるようにする
- 業務ルールの詳細、DTOの検証、DBスキーマ、テストケースは対象外とし、各仕様書へ委譲する

## 認証要否

| API | Bearer JWT | 備考 |
| --- | --- | --- |
| Todo API `/api/v1/todos/**` | 必須 | `Authorization: Bearer <access token>`。所有者はtokenの`sub`から決める |
| 認証API `/api/v1/auth/**` | 不要 | 登録・ログインは未認証で利用できる |
| OpenAPI・Swagger UI | 不要 | `/v3/api-docs`、`/swagger-ui.html` |

tokenなし・不正・期限切れのTodo API要求は`401 Unauthorized`とし、業務処理を実行しない。認証APIはtokenを要求しない。

## Todo API

| 操作 | HTTP契約 | 成功応答 |
| --- | --- | --- |
| Todo一覧 | `GET /api/v1/todos?page=1&size=5` | `200 OK`。Todo配列とページ情報 |
| Todo作成 | `POST /api/v1/todos`。本文にタイトルと任意の説明を含める | `201 Created`。作成したTodoと`Location`ヘッダー |
| Todo取得 | `GET /api/v1/todos/{todoId}` | `200 OK`。該当Todo |
| Todo更新 | `PUT /api/v1/todos/{todoId}`。タイトル・説明・状態を置き換える | `200 OK`。更新後のTodo |
| Todo削除 | `DELETE /api/v1/todos/{todoId}` | `204 No Content`。本文なし |

| 共通事項 | 契約 |
| --- | --- |
| ページ指定 | `page`は1始まり。`size`は既定5、1以上100以下。`page`が1未満、`size`が範囲外、または数値でない値は`400 Bad Request` |
| 並び順 | 更新日時降順、同時刻ならID降順 |
| Todo応答 | ID、タイトル、説明、状態、作成日時、更新日時を含める。所有者IDは含めない |
| 日時表現 | UTCのRFC 3339形式で返す |
| 所有者 | リクエストにユーザーIDを含めない。不存在と他ユーザー所有のTodoは同じ`404 Not Found`として扱う |
| 作成時の状態 | リクエストでは受け取らず、Serviceが`PENDING`に設定する |

DTOの具体的なrecord項目、入力検証、未知JSON項目の扱いは[DTO・Validation設計仕様](dto-validation.md)を参照する。

## 認証API

| 操作 | HTTP契約 | 成功応答 |
| --- | --- | --- |
| ユーザー登録 | `POST /api/v1/auth/register` | `201 Created`。ユーザー公開情報とBearer access token。`Location`ヘッダーは付けない |
| ログイン | `POST /api/v1/auth/login` | `200 OK`。Bearer access tokenと有効期間 |

- `tokenType`は固定で`Bearer`、`expiresIn`は900秒
- 登録時の`displayName`は省略できる。入力項目と検証は[DTO・Validation設計仕様](dto-validation.md)を参照する

認証方式、JWTのclaim、有効期限、認可境界は[認証・認可設計仕様](authentication-design.md)を参照する。

## エラー応答

- 入力検証、リクエスト本文の読み取り、Todo不存在、認証APIの資格情報不一致・重複は`application/problem+json`のRFC Problem Details形式で返す
- 項目別の入力エラーは`errors`配列に`field`と日本語`message`を含める。項目を特定できないエラーには`errors`を付けない
- Todo APIの`401`はResource Server標準の応答とし、`WWW-Authenticate: Bearer`を付ける。本文形式はこの契約では規定しない
- ログイン失敗の`401`はProblem Detailsで返し、Bearerで保護されたリソースではないため`WWW-Authenticate`を付けない
- パス・クエリの型不一致やページ指定の範囲外は`400 Bad Request`とする。本文はフレームワーク標準のエラー応答で、Problem Detailsを保証しない

### Todo APIのエラー

| 状況 | status | detail | errors拡張 |
| --- | --- | --- | --- |
| 入力制約違反 | `400 Bad Request` | `入力内容を確認してください` | `field`と日本語`message`を含む |
| 未知フィールド・JSON構文エラー・列挙値の変換エラー | `400 Bad Request` | `リクエスト本文を読み取れません` | 付けない |
| 不存在または他ユーザー所有のTodo | `404 Not Found` | `指定されたTodoが見つかりません` | 付けない |
| tokenなし・不正・期限切れ | `401 Unauthorized` | 規定しない | 付けない |

### 認証APIのエラー

| 状況 | status | detail | errors拡張 |
| --- | --- | --- | --- |
| 入力制約違反 | `400 Bad Request` | `入力内容を確認してください` | `field`と日本語`message`を含む |
| 未知フィールド・JSON構文エラー | `400 Bad Request` | `リクエスト本文を読み取れません` | 付けない |
| 登録済み`userName` | `409 Conflict` | `指定されたユーザー名は既に登録されています` | 付けない |
| ログイン資格情報の不一致 | `401 Unauthorized` | `ユーザー名またはパスワードが正しくありません` | 付けない。`WWW-Authenticate`も付けない |

## OpenAPI / Swagger UI

`springdoc-openapi`がControllerのHTTP契約とDTOからOpenAPI仕様を生成する。JSON仕様は`/v3/api-docs`、ブラウザでの確認画面は`/swagger-ui.html`（`/swagger-ui/index.html`へリダイレクト）で参照できる。エラー応答など実装から一意に推論できない契約はControllerのOpenAPI注釈で明示する。

認証・認可のOpenAPI契約:

- `OpenApiConfiguration`で`bearerAuth`（`type: http`、`scheme: bearer`、`bearerFormat: JWT`）を定義する
- `TodoController`に`@SecurityRequirement(name = "bearerAuth")`を付け、Todo操作の認証要件を明示する
- `AuthController`の登録・ログインには`@SecurityRequirement`を付けない
- AuthControllerには登録`201`・`400`・`409`、ログイン`200`・`400`・`401`の応答を注釈で明示する
