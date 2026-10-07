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
* **`Docker Desktop`** の起動（`Testcontainers` を用いたテスト実行に必須）
* **`Git`** による分散バージョン管理環境
* **`GitHub Copilot`** エージェントが利用可能な `IDE`（`Eclipse` 等）

## 🗺 開発ロードマップ

### 🟩 Step 1: 環境構築とテスト基盤の準備
- [x] `Git` リポジトリ初期化・`.github/copilot-instructions.md` および `README.md` の作成
- [ ] `build.gradle` の依存関係（`Spring Boot Starter`, `PostgreSQL`, `Testcontainers`, `Lombok` 等）の確認・追加
- [ ] `Docker Desktop` の起動確認と `Testcontainers` の仕組み理解
- [ ] `Testcontainers` を用いた `PostgreSQL` 接続確認テストの作成と実行（`GREEN` 確認）

### 🟩 Step 2: データ層（`JPA` / `Repository`）の `TDD` 実装
- [ ] `JPA` エンティティと `Repository`（`Spring Data JPA`）の役割理解
- [ ] `User` エンティティおよび `UserRepository` のテスト作成（`@DataJpaTest` + `Testcontainers`）（`RED`）
- [ ] `User` エンティティおよび `UserRepository` の実装（`GREEN`）
- [ ] `Todo` エンティティ（`@ManyToOne` リレーション含む）および `TodoRepository` のテスト作成（`RED`）
- [ ] `Todo` エンティティおよび `TodoRepository` の実装（`GREEN`）

### 🟩 Step 3: ドメイン・ビジネスロジック層（`Service`）の `TDD` 実装
- [ ] `Mockito` を用いたモック化（`@Mock`, `@InjectMocks`）と単体テスト手法の理解
- [ ] `UserService` の単体テスト作成（`RED`）と実装（`GREEN`）
- [ ] `TodoService` の `CRUD` 単体テスト作成（`RED`）と実装（`GREEN`）

### 🟩 Step 4: `JWT` 認証・認可基盤の構築
- [ ] `Spring Security` のフィルターチェーンとステートレス認証（`JWT`）の仕組み理解
- [ ] `JWT` 発行・検証ユーティリティ（`JwtTokenProvider` 等）の単体テスト作成（`RED`）と実装（`GREEN`）
- [ ] ログイン `API` および `AuthenticationFilter` の実装と動作検証

### 🟩 Step 5: API層（`Controller`）と `OpenAPI` の `TDD` 実装
- [ ] `Java 21` の `record`（`DTO`）定義と `Validation`（入力バリデーション）の整理
- [ ] `@WebMvcTest` と `MockMvc` を用いた `UserController` / `AuthController` のテスト作成（`RED`）と実装（`GREEN`）
- [ ] 認証保護された `TodoController` のテスト作成（`RED`）と実装（`GREEN`）
- [ ] `springdoc-openapi` による `Swagger UI` ドキュメント生成とブラウザでの動作検証

### 🟩 Step 6: 結合テスト（`Integration Test`）と総仕上げ
- [ ] `@SpringBootTest` + `Testcontainers` による一気通貫エンドツーエンドテストの作成・実行
- [ ] `./gradlew test` による全テストスイートの一括実行確認
- [ ] 最終コードレビューとドキュメント整理
