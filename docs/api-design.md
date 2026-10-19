# API設計仕様

個々のテストケース、期待結果、テストリンク、実行状態は[テストケース一覧](test-case-catalog.md)で管理する。

## 備考

- Section 3で実装したTodoServiceをHTTP APIとして公開し、DTO、入力検証、HTTPエラー、ページネーション、OpenAPIの役割と連携を学ぶ。
- 「Web API: The Good Parts」に通じるリソース指向・一貫性・利用者に明確な契約という一般的な設計思想を判断基準とする。
- HTTPメソッドの意味を保ち、Entityを外部へ公開しない。
- Section 4では認証機構を実装せず、ローカル動作確認用の所有者境界を設けた。Section 5で認証・認可へ接続する。
- Section 5では[認証・認可設計仕様](authentication-design.md)に従って認証済みユーザーからTodo所有者を解決し、認証前のローカルデモユーザー機能を置き換える。

## 仕様

| 操作 | HTTP契約 | 成功応答 |
| --- | --- | --- |
| Todo一覧 | `GET /api/v1/todos?page=1&size=5` | `200 OK`。Todo配列とページ情報 |
| Todo作成 | `POST /api/v1/todos`。本文にタイトルと任意の説明を含める | `201 Created`。作成したTodoと`Location`ヘッダー |
| Todo取得 | `GET /api/v1/todos/{todoId}` | `200 OK`。該当Todo |
| Todo更新 | `PUT /api/v1/todos/{todoId}`。タイトル・説明・状態を置き換える | `200 OK`。更新後のTodo |
| Todo削除 | `DELETE /api/v1/todos/{todoId}` | `204 No Content`。本文なし |

| 共通事項 | 合意内容 |
| --- | --- |
| ページ指定 | 公開`page`は1始まり。既定`size`は5、最大`size`は100。DB側でページ分割し、順序は更新日時降順、同時刻ならID降順 |
| Todo応答 | ID、タイトル、説明、状態、作成日時、更新日時を含める |
| 日時表現 | UTCのRFC 3339形式で返す |
| 所有者 | ユーザーIDをリクエストに含めない。不存在と他ユーザー所有のTodoは同じ`404 Not Found`として扱う |
| エラー | RFC Problem Details形式を基本とする。項目別入力エラーはAPI固有の拡張フィールドとして定義する |
| 作成時の状態 | リクエストでは受け取らず、Serviceが`PENDING`に設定する |
| ローカル確認データ | `local`プロファイルでのみ専用デモユーザー1人とTodo15件を起動時に初期化する。起動のたびにそのユーザーのTodoを置き換えるため、同ユーザーが追加・編集したTodoもリセットされる。認証前の学習用途に限り、本番や認証用アカウントには使用しない |

DTOの具体的なrecord項目、Validation、未知JSON項目の扱い、Problem Details拡張は[DTO・Validation設計仕様](dto-validation.md)を参照する。

## 認証API

認証APIは次の2操作で、契約・JWT・エラー応答の詳細は[認証・認可設計仕様](authentication-design.md)を正とする。

| 操作 | HTTP契約 | 成功応答 |
| --- | --- | --- |
| ユーザー登録 | `POST /api/v1/auth/register` | `201 Created`。ユーザー公開情報とBearer access token |
| ログイン | `POST /api/v1/auth/login` | `200 OK`。Bearer access tokenと有効期間 |

Todo APIはBearer JWTを要求し、認証APIはBearer tokenなしで利用できる。

## OpenAPI / Swagger UI

`springdoc-openapi`がControllerのHTTP契約とDTOからOpenAPI仕様を生成する。JSON仕様は`/v3/api-docs`、ブラウザでの確認画面は`/swagger-ui.html`で参照できる。エラー応答など実装から一意に推論できない契約はControllerのOpenAPI注釈で明示する。

認証・認可のOpenAPI契約:

- `OpenApiConfiguration`で`bearerAuth`（`type: http`、`scheme: bearer`、`bearerFormat: JWT`）を定義する
- `TodoController`に`@SecurityRequirement(name = "bearerAuth")`を付け、Todo操作の認証要件を明示する
- `AuthController`の登録・ログインには`@SecurityRequirement`を付けない
- AuthControllerには登録`201`・`400`・`409`、ログイン`200`・`400`・`401`の応答を注釈で明示する
