# DTO・バリデーション設計仕様

HTTP入出力のDTOと入力検証の契約を定義する。HTTPステータスとエラー応答は[API設計仕様](api-design.md)、認証・認可方式は[認証・認可設計仕様](authentication-design.md)、永続データの制約は[データモデル設計仕様](data-modeling.md)、テストケースと実行状態は[テストケース一覧](test-case-catalog.md)を正とする。

## 目的と範囲

- DTOはJava 21の`record`で定義し、EntityをHTTP入出力へ直接使わない
- Controller境界で入力形式を検証し、業務ルールはService、永続化制約はDBへ委ねる
- HTTPメソッド、ステータス、ヘッダー、エラー本文の詳細は対象外とし、[API設計仕様](api-design.md)へ委譲する

## 共通方針

- JSONの未知フィールドは無視せず`400 Bad Request`として拒否する。クライアントの項目名誤りや、契約外の状態・所有者IDを検出するため
- 入力検証はJakarta Bean Validationのアノテーションで宣言し、DTOのrecordコンポーネントに制約を付ける
- 入力検証エラーのHTTP応答と`errors`配列は[API設計仕様](api-design.md)に従う
- 前後空白を除去するDTOは、除去後の値に対して必須・長さ検証が行われるようrecordのコンパクトコンストラクタで正規化する

## Todo APIのDTO

| DTO | フィールド | 制約・挙動 |
| --- | --- | --- |
| `CreateTodoRequest` | `title`, `description` | タイトルは前後空白を除去した上で必須・空白のみ不可・最大100文字。説明は省略または`null`を許可。状態とユーザーIDは受け取らない |
| `UpdateTodoRequest` | `title`, `description`, `status` | 全項目を置き換える。タイトルは作成時と同じ検証。説明の省略または`null`は説明を削除。状態は必須で、`PENDING`、`IN_PROGRESS`、`COMPLETED`のいずれか |
| `TodoResponse` | `id`, `title`, `description`, `status`, `createdAt`, `updatedAt` | Todoの出力表現。日時はUTCのRFC 3339形式 |
| `TodoPageResponse` | `items`, `page`, `size`, `totalElements`, `totalPages` | `items`はTodo応答の配列。`page`は1始まり。Spring Dataの`Page`自体は公開しない |

## 認証APIのDTO

| DTO | フィールド | 制約・挙動 |
| --- | --- | --- |
| `RegisterRequest` | `userName`, `password`, `displayName` | `userName`は必須・小文字英数字のみ・最大50文字。`password`は必須・8文字以上・UTF-8で72バイト以下。`displayName`は任意・最大50文字 |
| `LoginRequest` | `userName`, `password` | 登録と同じ`userName`・`password`制約。`displayName`は受け取らない |
| `RegisterResponse` | `userName`, `displayName`, `accessToken`, `tokenType`, `expiresIn` | 登録成功の出力。`displayName`は未指定なら`null`。`id`、`password`、`role`は含めない |
| `TokenResponse` | `accessToken`, `tokenType`, `expiresIn` | ログイン成功の出力。ユーザー情報、`id`、`password`、`role`は含めない |

- `tokenType`は固定で`Bearer`、`expiresIn`は900秒
- `password`のUTF-8バイト長は`@Utf8ByteLength`で検証する。`null`は`@NotBlank`など必須チェックへ委ね、この制約では違反にしない

## 対象外

認証・認可の仕組み、DBスキーマとEntityマッピング、HTTPのステータス・ヘッダー・エラー本文、テストケースはこの文書では定義しない。
