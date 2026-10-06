# OpenAPI実装＆テスト駆動開発

テスト駆動開発（TDD）とベストプラクティスに基づいた実践的なREST APIを構築するための学習用プロジェクト。OpenAPI(`Swagger`)の自動生成やJWTを用いた認証・認可など

## 🎯 学習目標

このプロジェクトを通して以下のスキルと知識を習得します。

* **Spring Framework / Boot**: ベストプラクティスに沿ったディレクトリ設計と各種アノテーションの実践的な活用
* **Spring Data JPA & Hibernate**: JavaのデファクトスタンダードであるORMを用いたデータベース操作
* **自動テストとTDD (テスト駆動開発)**: JUnit 5とTestcontainersを活用し、テストから実装を進める開発サイクルの習得
* **OpenAPI / Swagger**: コードファースト（`springdoc-openapi`）によるAPI仕様書の自動生成と、DTOを用いた適切なデータ隠蔽
* **認証・認可**: Spring Securityを用いたJWT（JSON Web Token）ベースのステートレスな認証機構の構築
* **AI駆動開発**: Eclipse IDE内でGitHub Copilotを活用したシームレスな開発手法の確立

## 🛠 技術スタック

* **言語**: Java21
* **フレームワーク**: Spring Boot 4.x
* **ビルドツール**: Gradle (Groovy)
* **データベース**: PostgreSQL (開発環境)
* **ORM**: Hibernate(Spring Data JPA)
* **APIドキュメント**: springdoc-openapi (Swagger UI)
* **セキュリティ**: Spring Security (JWT)
* **テスト**: JUnit 5, Mockito, Testcontainers
* **その他**: Lombok, Spring Boot DevTools, Validation

## 📋 前提条件 (Prerequisites)

このプロジェクトをローカル環境で実行・テストするには、以下のツールが必要です。

* **Java 21** 以上のインストール
* **Docker Desktop** のインストールおよび起動（Testcontainersを用いたテスト実行に必須）
* **Git**
* Eclipse などの Java対応IDE

## 🗺 開発ロードマップ

|ステップ|フェーズ|概要|
|---|---|---|
|**1**|**環境構築とテスト基盤の準備**|Spring Bootプロジェクトの初期化。依存関係の整理とTestcontainersを用いたPostgreSQLコンテナ接続テストの成功（完了） |
|**2**|**データ層（JPA）の実装**|`@DataJpaTest` を用いてRepositoryのテストを記述し、EntityとRepositoryを実装してテストをパスさせる|
|**3**|**ビジネスロジック層の単体テスト**| JUnitとMockitoを使い、Service層の単体テストを記述。TodoのCRUDロジックを実装|
|**4**|**API層（Controller）とOpenAPI**|`@WebMvcTest` でHTTPリクエスト/レスポンスのテストを記述。Controller/DTOを実装し、SwaggerUIで仕様を確認|
|**5**|**JWT認証の導入とセキュリティテスト**|Spring Securityを導入。認証必須エンドポイントのテストを書き、JWT発行・検証フィルターを実装|
|**6**|**結合テストと総仕上げ**|`@SpringBootTest`でDBからAPIまで一気通貫の結合テストを実施。最終的な動作確認とドキュメント整備|