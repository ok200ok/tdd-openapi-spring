# データモデル設計仕様

永続化するデータの構造・制約・型・関係を定義する。HTTP契約は[API設計仕様](api-design.md)、DTOと入力検証は[DTO・Validation設計仕様](dto-validation.md)、認証・認可は[認証・認可設計仕様](authentication-design.md)、テストケースは[テストケース一覧](test-case-catalog.md)を正とする。

## データ上のルール

- `userName`はログインに使う一意な識別子とし、小文字英数字のみを保存する。Entityは保存前に小文字へ正規化し、DBの一意制約とCHECK制約でも重複・形式違反を防ぐ
- `displayName`は任意の表示用文字列とし、大文字小文字を保持する。一意制約は設けず、重複を許す
- Todoは必ず1人のUserに属する。`user_id`は非NULLの外部キーとし、所有者本人だけが操作できるかの認可はService/API層で行う
- Userを削除してもTodoを連鎖削除しない。削除カスケードは設定しない
- `password`にはハッシュ化済み文字列だけを保存し、平文パスワードは保存しない
- Todoの初期状態`PENDING`はDB DEFAULTにせず、Todoを作成するServiceが設定する。DBのNOT NULL制約は未設定値を拒否する

## テーブル定義

### `users`

| カラム | PostgreSQL型 | NULL | 制約・意味 |
| --- | --- | --- | --- |
| `id` | `UUID` | 不可 | 主キー。JPAプロバイダーが生成する |
| `user_name` | `VARCHAR(50)` | 不可 | 一意（`uk_users_username`）。小文字英数字のみをCHECK制約`ck_users_username_lower_alphanumeric`で保証する |
| `password` | `VARCHAR(255)` | 不可 | ハッシュ化済みパスワード |
| `display_name` | `VARCHAR(50)` | 可 | 表示名。重複可能 |
| `role` | `VARCHAR(50)` | 不可 | 権限名。例: `ROLE_USER` |
| `created_at` | `TIMESTAMP WITH TIME ZONE`（`timestamptz`） | 不可 | 作成時点。アプリケーションがUTC基準で設定する |
| `updated_at` | `TIMESTAMP WITH TIME ZONE`（`timestamptz`） | 不可 | 作成時・更新時点。アプリケーションがUTC基準で設定する |

### `todos`

| カラム | PostgreSQL型 | NULL | 制約・意味 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 不可 | 主キー。`IDENTITY`で採番する |
| `user_id` | `UUID` | 不可 | `users.id`への外部キー。所有者を表す |
| `title` | `VARCHAR(100)` | 不可 | Todoのタイトル |
| `description` | `TEXT` | 可 | 詳細説明。長さ上限を設けない |
| `status` | `VARCHAR(32)` | 不可 | Enum名を文字列で保存する。CHECK制約`ck_todos_status`で`PENDING`、`IN_PROGRESS`、`COMPLETED`に限定する |
| `created_at` | `TIMESTAMP WITH TIME ZONE`（`timestamptz`） | 不可 | 作成時点。アプリケーションがUTC基準で設定する |
| `updated_at` | `TIMESTAMP WITH TIME ZONE`（`timestamptz`） | 不可 | 作成時・更新時点。アプリケーションがUTC基準で設定する |

## リレーションと索引

| 関係・制約 | 内容 |
| --- | --- |
| `users` 1 — N `todos` | 1人のUserに複数のTodoが属する |
| 外部キー | `todos.user_id` → `users.id`。非NULLで、参照中Userの削除は拒否する |
| 削除時の挙動 | User削除時にTodoを連鎖削除しない |
| 索引 | `todos(user_id)`に所有者別検索用の索引`idx_todos_user_id`を設ける |

## 型と日時の方針

- `UUID`はUserの識別子に使い、アプリケーションコードで採番しない
- `Instant`で時点を表し、PostgreSQLの`timestamptz`へ対応付ける。実行環境のローカルタイムゾーンに依存しない
- `timestamptz`はタイムゾーン名を保持する型ではなく、時点を保存する型として扱う
- Enumは宣言順の数値ではなく名前を文字列で保存する。Enumの並べ替え・追加で既存データの意味が変わらないようにする

## Entityマッピング方針

| 項目 | マッピング・理由 |
| --- | --- |
| テーブルとカラム | `@Entity`と`@Table`でテーブルへ対応付け、`userName`は`@Column(name = "user_name")`で明示する。その他のcamelCaseプロパティはsnake_caseへ対応付ける |
| Userの主キー | `@GeneratedValue(strategy = GenerationType.UUID)`でUUIDをJPAプロバイダーに生成させる |
| Todoの主キー | `@GeneratedValue(strategy = GenerationType.IDENTITY)`でPostgreSQLのIDENTITY列に採番を任せる |
| Todoの状態 | `@Enumerated(EnumType.STRING)`でEnum名を文字列として保存する |
| Todoの所有者 | `@ManyToOne(fetch = FetchType.LAZY, optional = false)`と`@JoinColumn(name = "user_id", nullable = false)`で必須の関連を表す |
| User名・状態のCHECK | Hibernateの`@Check`でDB CHECK制約を定義し、アプリケーション外からの不正な書き込みも拒否する |
| 作成・更新時刻 | `@PrePersist`で作成・更新時刻を初期化し、`@PreUpdate`で更新時刻だけを進める。作成時刻は変更しない |

## 対象外

HTTP契約とエラー応答、DTOの項目と入力検証、認証・認可方式、テスト、本番運用のスキーマ移行は対象外とし、それぞれの仕様書を正とする。
