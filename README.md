# `OpenAPI` 実装 ＆ テスト駆動開発

テスト駆動開発（`TDD`）とベストプラクティスに基づいた実践的な `REST API` を構築するための学習用プロジェクトです。`OpenAPI`（`Swagger UI`）の自動生成や `JWT` を用いたステートレスな認証・認可機能を備えた `Todo` 管理バックエンドを構築します。

## 🎯 学習目標

このプロジェクトを通して以下のスキルと知識を習得します。

* **`Spring Framework` / `Spring Boot 4.x`**: ベストプラクティスに沿ったディレクトリ設計と各種アノテーションの実践的な活用
* **`Spring Data JPA` & `Hibernate`**: `Java` のデファクトスタンダードである `ORM` を用いたデータベース操作
* **自動テストと `TDD`（テスト駆動開発）**: `JUnit 5`, `Mockito`, `Testcontainers` を活用し、テストから実装を進める開発サイクルの習得
* **`OpenAPI` / `Swagger`**: コードファースト（`springdoc-openapi`）による `API` 仕様書の自動生成と、`Java 21` の `record`（`DTO`）を用いた適切なデータ隠蔽
* **認証・認可**: `Spring Security` を用いた `JWT`（`JSON Web Token`）ベースのステートレスな認証機構の構築
* **`AI` 駆動開発**: `Eclipse IDE` 内で `GitHub Copilot` エージェントを活用したシームレスな開発手法の確立

## 🛠 技術スタック

* **言語**: `Java 21`
* **フレームワーク**: `Spring Boot 4.x`
* **ビルドツール**: `Gradle` (`Groovy`)
* **データベース**: `PostgreSQL` (開発環境)
* **ORM**: `Spring Data JPA` (`Hibernate`)
* **APIドキュメント**: `springdoc-openapi` (`Swagger UI`)
* **セキュリティ**: `Spring Security` (`JWT`)
* **テスト**: `JUnit 5`, `Mockito`, `Testcontainers`
* **その他**: `Lombok`, `Spring Boot DevTools`, `Validation`

## 📋 前提条件 (Prerequisites)

このプロジェクトは学習目的のため、ローカル環境での実行・テスト検証を中心としています。

* **`Java 21`** のインストール
* **`Docker Desktop`** の起動（開発用PostgreSQLおよび`Testcontainers`に必須）
* **`GitHub Copilot`** エージェントが利用可能な `IDE`（`Zed` 等）

## 🐘 ローカル開発用データベース

開発時のPostgreSQLはDocker Composeで起動し、名前付きvolumeにデータを保持します。アプリケーション自体はローカルで実行します。テスト時は開発用DBを共有せず、Testcontainersが独立したPostgreSQLコンテナを起動します。

```powershell
docker compose up -d --wait
docker compose ps
mise exec -- .\gradlew.bat bootRun
```

アプリはホスト側ポート `localhost:5432` 経由で `api_todo` データベースに接続します（コンテナ内ポートも5432）。ホスト側5432番が他のPostgreSQLサーバーに使われている場合は、競合するサーバーを停止するか、Composeと `application.yaml` のポートを同じ別番号に変更してください。ローカル開発専用の接続情報は `compose.yaml` と `application.yaml` に定義しています。本番環境の認証情報として使用しないでください。

pgAdminから接続する場合は、新しいサーバー接続を登録し、Hostに `127.0.0.1`、Portに `5432`、Maintenance databaseに `api_todo`、Usernameに `api_todo` を指定します。pgAdminは接続クライアントであり、PostgreSQLサーバーとは別のものです。

```powershell
docker compose down
```

`down` はコンテナを停止・削除しますが、DBデータはvolumeに残ります。データも削除する場合に限り `docker compose down -v` を実行してください。

## 🗺 開発ロードマップ

### Section1: 環境構築とテスト基盤の準備
- [x] `Git` リポジトリ初期化・`.github/copilot-instructions.md` および `README.md` の作成
- [x] `build.gradle` の依存関係（`Spring Boot Starter`, `PostgreSQL`, `Testcontainers`, `Lombok` 等）の確認・追加
- [x] `Docker Desktop` の起動確認と `Testcontainers` の仕組み理解
- [x] `Testcontainers` を用いた `PostgreSQL` 接続確認テストの作成と実行（`GREEN` 確認）
- [x] Docker Composeによる開発用`PostgreSQL`と永続volumeの構成
- [x] 開発用DBの起動、アプリ接続、および停止後のデータ永続化確認

### Section2: データ層（`JPA` / `Repository`）の `TDD` 実装
- [ ] `JPA` エンティティと `Repository`（`Spring Data JPA`）の役割理解
- [ ] `User` エンティティおよび `UserRepository` のテスト作成（`@DataJpaTest` + `Testcontainers`）（`RED`）
- [ ] `User` エンティティおよび `UserRepository` の実装（`GREEN`）
- [ ] `Todo` エンティティ（`@ManyToOne` リレーション含む）および `TodoRepository` のテスト作成（`RED`）
- [ ] `Todo` エンティティおよび `TodoRepository` の実装（`GREEN`）

### Section3: ドメイン・ビジネスロジック層（`Service`）の `TDD` 実装
- [ ] `Mockito` を用いたモック化（`@Mock`, `@InjectMocks`）と単体テスト手法の理解
- [ ] `UserService` の単体テスト作成（`RED`）と実装（`GREEN`）
- [ ] `TodoService` の `CRUD` 単体テスト作成（`RED`）と実装（`GREEN`）

### Section4: `JWT` 認証・認可基盤の構築
- [ ] `Spring Security` のフィルターチェーンとステートレス認証（`JWT`）の仕組み理解
- [ ] `JWT` 発行・検証ユーティリティ（`JwtTokenProvider` 等）の単体テスト作成（`RED`）と実装（`GREEN`）
- [ ] ログイン `API` および `AuthenticationFilter` の実装と動作検証

### Section5: API層（`Controller`）と `OpenAPI` の `TDD` 実装
- [ ] `Java 21` の `record`（`DTO`）定義と `Validation`（入力バリデーション）の整理
- [ ] `@WebMvcTest` と `MockMvc` を用いた `UserController` / `AuthController` のテスト作成（`RED`）と実装（`GREEN`）
- [ ] 認証保護された `TodoController` のテスト作成（`RED`）と実装（`GREEN`）
- [ ] `springdoc-openapi` による `Swagger UI` ドキュメント生成とブラウザでの動作検証

### Section6: 結合テスト（`Integration Test`）と総仕上げ
- [ ] `@SpringBootTest` + `Testcontainers` による一気通貫エンドツーエンドテストの作成・実行
- [ ] `./gradlew test` による全テストスイートの一括実行確認
- [ ] 最終コードレビューとドキュメント整理
