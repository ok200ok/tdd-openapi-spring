# Section 2: データモデリングと永続化方針

この文書は、Section 2で実装する`User`と`Todo`のデータモデルを定義する。JavaのEntity、PostgreSQLのテーブル、およびRepositoryテストはこの設計に従う。HTTPのURL・メソッドやDTOの詳細はここでは定義せず、ControllerのTDDを始めるSection 4より前に別途確定する。

## ドメイン上のルール

- `userName`はログインに使う一意な識別子。`displayName`とは別の値であり、表示名は重複できる。
- APIの`userName`入力は小文字英数字のみを受け入れる。Entityも保存前に小文字へ正規化し、DBの一意制約で同一ユーザー名の重複を防ぐ。
- `displayName`は任意の表示文字列で、大文字小文字を保持し、一意制約を設けない。
- 各Todoは必ず1人のUserに属する。Todoの読み書きは所有者本人に限定する。所有者認可はService/API層で実装し、EntityやDB制約だけに任せない。
- 現スコープにUser削除機能は設けず、TodoをUserと連鎖削除しない。
- `password`列にはハッシュ化済み文字列を保存する。ハッシュ化・検証はService/Security層の責務で、API応答やログには出さない。

## PostgreSQLテーブル設計

### `users`

| カラム | PostgreSQL型 | NULL | 制約・意味 |
| --- | --- | --- | --- |
| `id` | `UUID` | 不可 | 主キー。JPAプロバイダーが生成 |
| `user_name` | `VARCHAR(50)` | 不可 | 一意。小文字英数字のみを保存し、DB CHECK制約で形式を保証 |
| `password` | `VARCHAR(255)` | 不可 | ハッシュ化済みパスワード |
| `display_name` | `VARCHAR(50)` | 可 | 表示名。重複可能 |
| `role` | `VARCHAR(50)` | 不可 | 例: `ROLE_USER` |
| `created_at` | `TIMESTAMP WITH TIME ZONE` (`timestamptz`) | 不可 | 作成時にUTC基準の時点をアプリケーションが設定 |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` (`timestamptz`) | 不可 | 作成時と更新時にUTC基準の時点をアプリケーションが設定 |

Javaでは両プロパティを`Instant`で表し、実行環境のローカルタイムゾーンに依存しない時点として扱う。PostgreSQLの`timestamptz`は元のタイムゾーン名を保持する型ではなく、時点を保存する型である。

Javaプロパティは`userName`とし、`@Column(name = "user_name")`でDBカラムへ明示的に対応付ける。その他のcamelCaseプロパティはsnake_caseカラムへ対応付ける。UserのUUIDはアプリケーションコードでなく、`@GeneratedValue(strategy = GenerationType.UUID)`を介してJPAプロバイダーが生成する。

### `todos`

| カラム | PostgreSQL型 | NULL | 制約・意味 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 不可 | 主キー。`IDENTITY`で採番 |
| `user_id` | `UUID` | 不可 | `users.id`への外部キー。User削除時にTodoを連鎖削除しない |
| `title` | `VARCHAR(100)` | 不可 | Todoタイトル |
| `description` | `TEXT` | 可 | 詳細説明 |
| `status` | `VARCHAR(32)` | 不可 | Enum名を文字列で保存。DB CHECK制約で`PENDING`, `IN_PROGRESS`, `COMPLETED`に限定 |
| `created_at` | `TIMESTAMP WITH TIME ZONE` (`timestamptz`) | 不可 | 作成時にUTC基準の時点をアプリケーションが設定 |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` (`timestamptz`) | 不可 | 作成時と更新時にUTC基準の時点をアプリケーションが設定 |

`todos(user_id)`に所有者ごとの検索用インデックスを設ける。JPA関連は必須の`@ManyToOne(fetch = LAZY)`とし、削除カスケードは設定しない。Todo作成時の初期状態`PENDING`は後続のServiceが明示的に設定し、Repository/Entityは指定された状態を永続化する。DBのDEFAULTにはせず、`NOT NULL`制約で未設定値を拒否する。これにより業務上の初期値をアプリケーション側で一元管理し、JPA Entityの状態とDBの状態を揃える。Todoの初期状態`PENDING`はDB DEFAULTにせず、Todoを作成するServiceが明示的に設定する。DBの`NOT NULL`制約は未設定値を拒否する役割とする。

## 実装・テスト方針

- Entityのライフサイクルコールバックで`createdAt`と`updatedAt`を設定・更新する。時刻テストでは厳密な時刻一致を要求せず、処理前後の範囲・前進を確認する。
- DB制約とEntityのマッピングを一致させ、RepositoryテストではPostgreSQL 17 Testcontainersに対して保存・再取得・一意性・必須項目・最大長・外部キーを検証する。Service/APIのBean Validationや認可は各レイヤーのテストで扱う。
- ローカル学習環境ではHibernateの`update`でEntity変更をDBへ反映する。これは開発用の便宜であり、本番運用にはバージョン管理されたスキーママイグレーションを用いる。
- `@DataJpaTest`は既定で組み込みDBへ置き換えるため、既存のPostgreSQL Testcontainers設定をimportし、DB置換を無効化する。テスト用DBのスキーマは`create-drop`で毎回作成・破棄し、各テストはトランザクションロールバックで分離する。
- Section 2ではUser/Todo Entityと最小限のSpring Data Repositoryまでを実装し、業務処理・認証・HTTP仕様は先取りしない。

## Section 2のタスクとガイド順序

READMEのSection 2の各チェックボックスは、それぞれ開発者と一緒に完了させる一つのタスクに対応する。現在のコードとテストは既に存在するが、エージェントが先行実装したため、チェックボックスはガイド付きの確認が済むまで未完了のままとする。既存コードを消さず、設計・テスト・実装の理由と挙動を順番に振り返る。


1. **データモデルの確認**: User/Todoのユースケースと制約、特に`userName`と`displayName`、Todoの所有権、物理カラム設計を開発者と確認し、必要ならこの文書を修正する。
2. **JPAとテスト基盤の理解**: EntityとRepositoryの責務を整理し、`@DataJpaTest`、Testcontainers、組み込みDB置換の無効化、テストトランザクションの動きを既存テストと設定から読み解く。
3. **User基本マッピング**: UUID、カラム制約、任意の`displayName`、タイムスタンプを扱うUserのテストとEntity/Repository実装を読み、開発者が該当テストを実行して確認する。
4. **ユーザー名検索と制約**: 正規化、許容形式、一意制約、検索メソッドのテストと実装を読み、DB制約とEntity側の正規化の役割を区別する。
5. **Todo基本マッピング**: ID採番、タイトル・説明・状態、Enumの文字列保存、タイムスタンプを扱うテストと実装を読む。
6. **TodoとUserのリレーション**: 非NULL外部キー、`@ManyToOne(fetch = LAZY)`、削除制約、関連Entityの保存順を検証するテストと実装を読む。所有者別取得のRepositoryメソッドがまだない場合は、Service設計に必要な問い合わせ方法として範囲を確認し、必要性を合意した上で扱う。
7. **Section 2の検証**: 開発者がRepositoryテストと全テストを実行し、実行結果を既存の実装・設計文書と照合する。

この振り返りでは既存のテストがGREENであることを確認するが、それを開発者がREDから始めたTDDサイクルだったとは扱わない。次Section以降の新規開発では、各タスクの振る舞いごとにRED、最小実装でGREEN、振る舞いを変えないRefactor、その後の再実行の順を開発者自身が確認する。追加の振る舞いを実装する際は、新しいテストから次のサイクルを始める。

テストのRedは実行時アサーション失敗だけでなく、対象のプロダクト型がまだないことによるコンパイル失敗から始まる場合がある。型を加えた後は必ず実行可能な永続化テストにし、Greenを確認する。
