# `OpenAPI` 実装 ＆ テスト駆動開発

## 🎯 学習目標

このプロジェクトを通して以下のスキルと知識の習得を目指す

- **自動テストとテスト駆動開発**: `JUnit 5`, `Mockito`の使用、 `Testcontainers` によるテスト専用分離環境の構築
- **AI駆動開発**: `AGENTS.md`によるエージェント制御、各`/`コマンド使い分けの実践（今回は `GitHub Copilot` Freeプランを使用）
- **迅速な環境構築**: `Mise`による言語バージョン統一（`Java 21`）、`Docker Compose`による開発用DBとテスト用DBの立ち上げ
- **CIとリポジトリ安全性**: リモートリポジトリに対して`Github Actions`によるCI、`main`ブランチ保護ルールセット策定などで安全性を確保
- **`Spring Framework` / `Spring Boot 4.x`**: ベストプラクティスに沿ったレイヤードアーキテクチャ、ディレクトリ設計、各種アノテーション
- **`Spring Data JPA`仕様 と `Hibernate`**: `Spring` においてよりデファクトであるらしい `Hibernate`を用いたデータベース操作（MyBatisORMは学習済みのため）
- **`OpenAPI` / `Swagger`**: コードファースト（`springdoc-openapi`）による `API` 仕様ドキュメントサイトの自動生成と、の `record`（`DTO`）を用いた適切なデータ隠蔽
- **認証・認可**: `Spring Security` モジュールで `JWT`（`JSON Web Token`）ベースのステートレスな認証機構の構築

## 🛠 技術スタック

- **言語**: `Java 21`
- **フレームワーク**: `Spring Boot 4.x`
- **ビルドツール**: `Gradle` (`Groovy`)
- **DB**: `Docker Compose`, `PostgreSQL` (開発環境とテスト環境)
- **ORM**: `Spring Data JPA` (`Hibernate`)
- **APIドキュメント**: `springdoc-openapi` (`Swagger UI`)
- **認証**: `Spring Security` (`JWT`)
- **テスト**: `JUnit 5`, `Mockito`, `Testcontainers`
- **CI**: `Github Actions`
- **その他**: `Lombok`, `Spring Boot DevTools`, `Validation`
- **コーディングエージェント**: `GitHub Copilot`

## 📋 前提条件 (Prerequisites)

このプロジェクトは学習目的のため、ローカル環境での実行・テスト検証を中心としています。

- **`mise` CLI**（Java 21の導入とバージョン管理に使用）
- **Dockerクライアント** の起動（`Docker Desktop`や`OrbStack`など。開発用PostgreSQLおよび`Testcontainers`による分離テスト環境に必須）

## ☕ MiseによるJava環境の準備

このプロジェクトでは `mise.toml` でJava 21（Temurin）を指定しています。初回は次の手順でmiseとプロジェクト指定のJavaを準備してください。

1. [mise公式ドキュメント](https://mise.jdx.dev/getting-started.html)に従ってmise CLIをインストールします
2. リポジトリをクローンし、プロジェクトのルートディレクトリへ移動します
3. `mise.toml` の内容を確認してから、下記コマンドで特定バージョンの言語やツールをインストールします

```sh
mise install
```
> ”既にmise.tomlファイルが存在するプロジェクトに参加する場合、まずmiseをインストールし、プロジェクトのmise.tomlファイルを読み、次にプロジェクトディレクトリでmise installを実行します。”

4. その後、[シェルでmiseを有効化](https://mise.jdx.dev/getting-started.html#activate-mise)します。

## 🚀 起動方法

### 🐋 開発用DB

開発時のPostgreSQLはDocker Composeで起動し、名前付きvolumeにデータを保持します。アプリケーション自体はローカルで実行します。テスト時は開発用DBを共有せず、Testcontainersが独立したPostgreSQLコンテナを起動します。

ローカル開発専用の接続情報は `compose.yaml` と `application.yaml` に定義しています。**本番環境の認証情報として使用しないでください**

```
docker compose up -d --wait
docker compose ps
```

コンテナ終了は

```
docker compose down
```

`down` はコンテナを停止・削除しますが、DBデータはvolumeに残ります。データも削除する場合に限り `docker compose down -v` を実行してください。

### 🐘 開発用Springサーバー

miseをシェルで有効化済みなら、`mise exec --`を省略できるので、

↓macOS / Linuxなら
```sh
./gradlew bootRun
```

↓Windowsなら
```powershell
.\gradlew.bat bootRun
```

いずれもGradle Wrapperがプロジェクト指定のGradleを使用します。

## 🗺 開発ロードマップ

### タスクの進め方

各チェックボックスを一つの学習タスクとして扱い、次の段階を小分けに進める。各段階で説明と確認を行い、開発者の質問・合意を待ってから次へ進む。Entity、Repository、Service、Controllerなどの役割単位は「レイヤーコンポーネント」と呼び、個々の責務だけでなく相互の依存・協調関係と全体の開発フロー上の位置づけも学ぶ。

1. 目的・範囲・仕様・受け入れ条件を確認し、曖昧な前提を合意する
2. テストケースと期待結果を決め、何をどのレイヤーで検証するか合意する
3. 新規機能はテストコードだけを作成する。開発者が実行してREDを確認し、結果を共有するまで本番コードへ進まない
4. REDの理由を確認後、最小限の実装を行い、重要なコードと設計理由を解説する。開発者がGREENを確認した後、必要なら振る舞いを保つRefactorを行い、再実行する
5. 必要な追加検証やドキュメント更新を行い、結果を照合してタスク完了を判断する

RED/GREENの確認は開発者が実行結果を共有した場合に限る。機能上のREDと環境・ビルド上の失敗を区別し、Refactorは必要な場合のみ行う。学習価値のあるクラス・メソッド・アノテーションの役割や理由は、冗長にならないコメント/Javadocと段階的なガイドで説明する。

Section 2は先行実装済みコードを振り返る例外であり、実施していないTDDのRED/GREENを演出しない。既存コードとテストを段階的に読み解き、開発者自身によるテスト実行で挙動を確認する。理解を確認するまではチェック項目を完了扱いにしない。

### Section1: 環境構築とテスト基盤の準備
- [x] `Git` リポジトリ初期化・`.github/copilot-instructions.md` および `README.md` の作成
- [x] `build.gradle` の依存関係（`Spring Boot Starter`, `PostgreSQL`, `Testcontainers`, `Lombok` 等）の確認・追加
- [x] `Docker Desktop` の起動確認と `Testcontainers` の仕組み理解
- [x] `Testcontainers` を用いた `PostgreSQL` 接続確認テストの作成と実行（`GREEN` 確認）
- [x] Docker Composeによる開発用`PostgreSQL`と永続volumeの構成
- [x] 開発用DBの起動、アプリ接続、および停止後のデータ永続化確認
- [x] GitHub ActionsによるCIパイプラインの構築（作業ブランチへのpushと`main`向けPRでGradleテストおよびTestcontainersを実行し、GitHub上で成功・失敗を確認）
- [ ] `main`ブランチの保護と必須CIチェックの設定（リポジトリをPublicにするまではルール有効かができないため保留）

### Section2: データ層（`JPA` / `Repository`）の段階的な振り返り
- [x] `User` / `Todo` のユースケース、所有権、カラム・制約を含むデータモデルを確認・確定する（[`docs/contexts/section2-data-modeling.md`](docs/contexts/section2-data-modeling.md)）
- [ ] JPA EntityとSpring Data Repositoryの役割、および`@DataJpaTest`がPostgreSQL Testcontainersを使う仕組みを理解する
- [ ] Userの基本マッピング（UUID、必須・任意カラム、長さ、作成・更新時刻）をRepositoryテストで確認する
- [ ] `userName`の正規化・形式・一意性と検索をテストし、UserRepositoryの役割を確認する
- [ ] Todoの基本マッピング（採番ID、タイトル・説明・状態、作成・更新時刻）をRepositoryテストで確認する
- [ ] TodoとUserの必須リレーション、外部キー、遅延ロード、削除制約をテストし、所有者別データアクセスの土台を確認する
- [ ] Section 2のRepositoryテストと既存テストを実行し、結果とデータモデル文書・チェック項目を照合する

### Section3: ドメイン・ビジネスロジック層（`Service`）の `TDD` 実装
- [ ] `Mockito` を用いたモック化（`@Mock`, `@InjectMocks`）と単体テスト手法の理解
- [ ] `UserService` の単体テスト作成（`RED`）と実装（`GREEN`）
- [ ] `TodoService` の `CRUD` 単体テスト作成（`RED`）と実装（`GREEN`）

### Section4: `JWT` 認証・認可基盤の構築
- [ ] `Spring Security` のフィルターチェーンとステートレス認証（`JWT`）の仕組み理解
- [ ] `JWT` 発行・検証ユーティリティ（`JwtTokenProvider` 等）の単体テスト作成（`RED`）と実装（`GREEN`）
- [ ] ログイン `API` および `AuthenticationFilter` の実装と動作検証

### Section5: API層（`Controller`）と `OpenAPI` の `TDD` 実装
- [ ] URL・HTTPメソッド・DTO・ステータスコード・エラー形式・認証要件を含むAPI契約をControllerテスト作成前に設計
- [ ] `Java 21` の `record`（`DTO`）定義と `Validation`（入力バリデーション）の整理
- [ ] `@WebMvcTest` と `MockMvc` を用いた `UserController` / `AuthController` のテスト作成（`RED`）と実装（`GREEN`）
- [ ] 認証保護された `TodoController` のテスト作成（`RED`）と実装（`GREEN`）
- [ ] `springdoc-openapi` による `Swagger UI` ドキュメント生成とブラウザでの動作検証

### Section6: 結合テスト（`Integration Test`）と総仕上げ
- [ ] `@SpringBootTest` + `Testcontainers` による一気通貫エンドツーエンドテストの作成・実行
- [ ] `./gradlew test` による全テストスイートの一括実行確認
- [ ] 最終コードレビューとドキュメント整理
