# Section 4: API設計

個々のテストケース、期待結果、テストリンク、実行状態は[テストケース一覧](test-case-catalog.md)で管理する。

## 目的と範囲

Section 3で実装したTodoServiceをHTTP APIとして公開し、DTO、入力検証、HTTPエラー、ページネーション、OpenAPIの役割と連携を学ぶ。APIはリソース指向で設計し、HTTPメソッドの意味を保ち、Entityを外部へ公開しない。Section 4では認証機構を実装せず、ローカル動作確認用の所有者境界を設ける。認証・認可への接続はSection 6で扱う。

## API設計

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
| ローカル確認データ | ローカル専用のユーザー1人とTodo約15件を用意する |

「Web API: The Good Parts」に通じるリソース指向・一貫性・利用者に明確な契約という一般的な設計思想を判断基準とする。
