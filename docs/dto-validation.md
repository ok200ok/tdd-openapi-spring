# DTO・Validation設計仕様

## 備考

- DTOはJava 21の`record`で定義し、EntityをHTTP入出力に直接使わない。
- JSONの未知フィールドは無視せず、`400 Bad Request`として拒否する。これによりクライアントの項目名誤りや、作成時に指定された状態・所有者IDなど契約外の入力を検出する。
- 入力検証エラーは`application/problem+json`のRFC Problem Details形式とし、API拡張の`errors`配列に項目名`field`と日本語の利用者向け`message`を含める。JSON構文エラーなど入力項目を特定できない場合は、項目別`errors`なしのProblem Detailsを返す。

## 仕様

| DTO | フィールド | 制約・挙動 |
| --- | --- | --- |
| `CreateTodoRequest` | `title`, `description` | タイトルは前後空白を除去した上で必須・空白のみ不可・最大100文字。説明は省略または`null`を許可。状態とユーザーIDは受け取らない |
| `UpdateTodoRequest` | `title`, `description`, `status` | 全項目を置き換える。タイトルは作成時と同じ検証。説明の省略または`null`は説明を削除。状態は必須で、`PENDING`、`IN_PROGRESS`、`COMPLETED`のいずれか |
| `TodoResponse` | `id`, `title`, `description`, `status`, `createdAt`, `updatedAt` | Todoの出力表現。日時はUTCのRFC 3339形式 |
| `TodoPageResponse` | `items`, `page`, `size`, `totalElements`, `totalPages` | `items`はTodo応答の配列。`page`は1始まり。Spring Dataの`Page`自体は公開しない |
