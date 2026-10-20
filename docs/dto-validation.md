# DTO・バリデーション設計仕様

## 備考

- DTOはJava 21の`record`で定義し、EntityをHTTP入出力に直接使わない。
- JSONの未知フィールドは無視せず、`400 Bad Request`として拒否する。これによりクライアントの項目名誤りや、作成時に指定された状態・所有者IDなど契約外の入力を検出する。
- 入力検証エラーは`application/problem+json`のRFC Problem Details形式とし、API拡張の`errors`配列に項目名`field`と日本語の利用者向け`message`を含める。JSON構文エラーなど入力項目を特定できない場合は、項目別`errors`なしのProblem Detailsを返す。

## TodoAPIのDTO

| DTO | フィールド | 制約・挙動 |
| --- | --- | --- |
| `CreateTodoRequest` | `title`, `description` | タイトルは前後空白を除去した上で必須・空白のみ不可・最大100文字。説明は省略または`null`を許可。状態とユーザーIDは受け取らない |
| `UpdateTodoRequest` | `title`, `description`, `status` | 全項目を置き換える。タイトルは作成時と同じ検証。説明の省略または`null`は説明を削除。状態は必須で、`PENDING`、`IN_PROGRESS`、`COMPLETED`のいずれか |
| `TodoResponse` | `id`, `title`, `description`, `status`, `createdAt`, `updatedAt` | Todoの出力表現。日時はUTCのRFC 3339形式 |
| `TodoPageResponse` | `items`, `page`, `size`, `totalElements`, `totalPages` | `items`はTodo応答の配列。`page`は1始まり。Spring Dataの`Page`自体は公開しない |

## 認証APIのDTO

登録・ログインのHTTPステータスとエラー応答は[API設計仕様](api-design.md)、認証方式とJWTは[認証・認可設計仕様](authentication-design.md)を参照する。

| DTO | フィールド | 制約・挙動 |
| --- | --- | --- |
| `RegisterRequest` | `userName`, `password`, `displayName` | `userName`は必須・小文字英数字のみ・最大50文字。`password`は必須・8文字以上・UTF-8で72バイト以下。`displayName`は任意・最大50文字 |
| `LoginRequest` | `userName`, `password` | 登録と同じ`userName`・`password`制約。`displayName`は受け取らない |
| `RegisterResponse` | `userName`, `displayName`, `accessToken`, `tokenType`, `expiresIn` | 登録成功の出力。`displayName`は未指定なら`null`。`id`、`password`、`role`は含めない |
| `TokenResponse` | `accessToken`, `tokenType`, `expiresIn` | ログイン成功の出力。ユーザー情報、`id`、`password`、`role`は含めない |

- `tokenType`は固定で`Bearer`、`expiresIn`は900秒
- `password`のUTF-8バイト長は`@Utf8ByteLength`で検証する。`null`は`@NotBlank`など必須チェックへ委ね、この制約では違反にしない
- 認証APIの入力検証エラーはTodo APIと同じ`errors`配列（`field`、`message`）を使う
- 登録済み`userName`の`409`、ログイン失敗の`401`は`errors`拡張を付けない
