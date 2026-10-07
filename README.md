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

| ステップ | フェーズ | 概要 |
| :--- | :--- | :--- |
| **1** | **環境構築とテスト基盤の準備** | `Spring Boot` プロジェクトの初期化。依存関係の整理と `Testcontainers` を用いた `PostgreSQL` コンテナ接続テストの成功 |
| **2** | **データ層（`JPA`）の実装** | `@DataJpaTest` と `Testcontainers` を用いて `User` および `Todo` の `Repository` テストを記述し、`Entity` と `Repository` を実装してテストをパスさせる |
| **3** | **ドメイン・ビジネスロジック層の実装** | `JUnit 5` と `Mockito` を使い、`UserService` および `TodoService` の高速単体テストを記述。データ検証・ビジネスロジックを実装 |
| **4** | **`JWT` 認証・認可基盤の構築** | `Spring Security` を導入。認証・認可用フィルターと `JWT` 発行・検証ロジックのテストを書き、ログイン `API` を実装 |
| **5** | **API層（`Controller`）と `OpenAPI`** | `@WebMvcTest` で認証保護された `API` リクエスト/レスポンスのテストを記述。`Controller`・`DTO`（`record`）を実装し、`Swagger UI` で動作確認 |
| **6** | **結合テストと総仕上げ** | `@SpringBootTest` で `DB` から `API` エンドポイントまで一気通貫の結合テストを実施。動作確認とドキュメント整備 |
