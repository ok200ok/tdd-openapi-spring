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

### 📚 Javadoc（公開APIドキュメント）

ソース内のJavadocから、公開クラス・メソッドの契約文書をローカル生成できます。出力先は `build/docs/javadoc` で、`private` は含みません。

```sh
./gradlew javadoc
open build/docs/javadoc/index.html
```

Windowsの場合は `.\gradlew.bat javadoc` を実行し、生成された `build/docs/javadoc/index.html` をブラウザで開きます。


## 🗺 開発ロードマップ

### タスクの進め方

ロードマップのチェックボックス一つを一つの学習タスクとして扱い、後続タスクを先回りせず一つずつ進める。各段階で説明後に停止し、質問と明確な合意を待つ（`LGTM`などの明示的な承認は合意として扱うが、応答がないことを承認とみなさない）。Entity、Repository、Service、Controllerなどの役割単位は「レイヤーコンポーネント」と呼び、各自の責務だけでなく相互の依存・協調関係と全体の開発フロー上の位置づけも学ぶ。

1. **目的・仕様**: 範囲、仕様、選択肢と理由、受け入れ条件を合意する。曖昧な点は質問し、推測で確定しない
2. **テストケース**: 期待結果、対象レイヤー、検証境界・テスト種別を合意し、[テストケース一覧](docs/test-case-catalog.md)を更新する
3. **テストコード・RED**: 新規機能ではテストコードだけを作成し、実行コマンドと期待する機能上のREDを提示して停止する。開発者が実行結果を共有するまで本番コードへ進まない
4. **分析・最小実装**: REDの理由を確認後、最小限の本番コードを実装して設計理由を解説し、学習上重要な説明を日本語コメント/Javadocにも残す。GREENの実行コマンドと期待結果を示して停止する
5. **GREEN・Refactor**: 開発者の実行結果を確認する。必要な場合に限り振る舞いを保つRefactorを行い、再実行結果を待つ
6. **追加確認・完了**: 必要な検証・文書更新を行う。合意ケース、テスト名、期待結果、リンク、状態を一覧と照合し、開発者と結果を確認してからタスクを完了扱いにする

機能上のREDとDocker・依存関係・ビルドなど環境上の失敗を区別し、環境問題を機能上のREDとして扱わない。RED/GREENの確認は開発者が実行結果を共有した場合に限る。Refactorは必要な場合のみ行う。TDDを適用できない既存実装の読解では、実施していないRED/GREENを演出せず、目的に合った確認手順に置き換える。

学習対象のコードでは、初学者が別途調べないと意味や影響を理解しにくいクラス・メソッド・アノテーションとその設定属性について、役割・採用理由・動作への影響を段階的に解説し、日本語コメント/Javadocにも残す。タスクに合意したテストケースは一覧に記録し、テストコードや解説の変更時にも内容・リンク・状態を同期する。

### Section1: 環境構築とテスト基盤の準備
- [x] `Git` リポジトリ初期化・`.github/copilot-instructions.md` および `README.md` の作成
- [x] `build.gradle` の依存関係（`Spring Boot Starter`, `PostgreSQL`, `Testcontainers`, `Lombok` 等）の確認・追加
- [x] `Docker Desktop` の起動確認と `Testcontainers` の仕組み理解
- [x] `Testcontainers` を用いた `PostgreSQL` 接続確認テストの作成と実行（`GREEN` 確認）
- [x] Docker Composeによる開発用`PostgreSQL`と永続volumeの構成
- [x] 開発用DBの起動、アプリ接続、および停止後のデータ永続化確認
- [x] GitHub ActionsによるCIパイプラインの構築（作業ブランチへのpushと`main`向けPRでGradleテストおよびTestcontainersを実行し、GitHub上で成功・失敗を確認）
- [x] `main`ブランチの保護と必須CIチェックの設定

### Section2: データ層（`JPA` / `Repository`）の段階的な振り返り
- [x] `User` / `Todo` のユースケース、所有権、カラム・制約を含むデータモデルを確認・確定する（[`データモデル設計仕様`](docs/data-modeling.md)）
- [x] JPA EntityとSpring Data Repositoryの役割、および`@DataJpaTest`がPostgreSQL Testcontainersを使う仕組みを理解する
- [x] Userの基本マッピング（UUID、必須・任意カラム、長さ、作成・更新時刻）をRepositoryテストで確認する
- [x] `userName`の正規化・形式・一意性と検索をテストし、UserRepositoryの役割を確認する
- [x] Todoの基本マッピング（採番ID、タイトル・説明・状態、作成・更新時刻）をRepositoryテストで確認する
- [x] TodoとUserの必須リレーション、外部キー、遅延ロード、削除制約をテストし、所有者別データアクセスの土台を確認する
- [x] Section 2のRepositoryテストと既存テストを実行し、結果とデータモデル文書・チェック項目を照合する

### Section3: ドメイン・ビジネスロジック層（`Service`）の `TDD` 実装
- [x] `Mockito` を用いたモック化（`@Mock`, `@InjectMocks`）と単体テスト手法の理解
- [x] Todo作成のService単体テスト作成（`RED`）と実装（`GREEN`）
- [x] 所有者を指定したTodoのID検索のService単体テスト作成（`RED`）と実装（`GREEN`）
- [x] 所有者別Todo一覧のService単体テスト作成（`RED`）と実装（`GREEN`）
- [x] Todo更新のService単体テスト作成（`RED`）と実装（`GREEN`）
- [x] Todo削除のService単体テスト作成（`RED`）と実装（`GREEN`）

### Section4: API層（`Controller`）と `OpenAPI` の `TDD` 実装
- [x] Todo API契約の設計（URL、メソッド、入出力、ステータス、エラーの合意）
- [x] UTC基準の日時型への見直し（Entityの日時型をInstant、PostgreSQL列をtimestamptzとする設計・実装。Repositoryテストで永続化と時点を確認）
- [x] Todo一覧のDBページネーション（RepositoryでDBページ分割し、Serviceが所有者条件とページ指定を委譲。公開APIは1始まり、既定サイズ5、最大100。Repository実DBテストとService単体テストで確認）
- [x] DTO・Validationの詳細設計（Java record DTO、入力制約、Problem Detailsの入力エラー拡張を合意し、[DTO仕様](docs/dto-validation.md)と[テストケース一覧](docs/test-case-catalog.md)へ記録）
- [x] TodoControllerのTDD（`@WebMvcTest`／`MockMvc`でHTTP境界、Validation、応答とService連携をテストし、RED・GREENを確認。仕様は[DTO仕様](docs/dto-validation.md)、合意ケースは[テストケース一覧](docs/test-case-catalog.md)を参照）
- [x] ローカル所有者境界と初期データ（ローカル専用ユーザー・Todo約15件を用意し、認証前の所有者ID解決からServiceへの受け渡しを確認）
- [x] OpenAPI／Swagger UI（`springdoc-openapi`で`/v3/api-docs`のAPI仕様を生成し、`/swagger-ui.html`でエンドポイントとスキーマを確認）

### Section5: 認証・認可基盤の追加
- [x] [`認証・認可設計仕様`](docs/authentication-design.md)と[`API設計仕様`](docs/api-design.md)を参照し、`Spring Security`の標準機能を使う認証方式とAPI契約（登録・ログイン、パスワードハッシュ、JWT、認可境界、エラー応答）を合意する
- [x] ユーザー登録とパスワードハッシュ化を担う`UserService`の単体テスト作成（`RED`）と実装（`GREEN`）
- [x] `JWT` 発行・検証ユーティリティ（`JwtTokenProvider` 等）の単体テスト作成（`RED`）と実装（`GREEN`）
- [x] ステートレスな`Spring Security`設定とBearer認証を実装し、認証済みユーザーのIDをTodo APIへ渡す（MVCテストで確認）
- [x] 本番用`JwtEncoder`／`JwtDecoder` Beanと署名鍵設定を追加して`@SpringBootTest`とCIを成功させ、既存Controllerテストを共通テスト設定の実Bearer tokenによる認証前提へ移行する
- [x] 登録・ログインAPIを実装してJWTを発行し、認証・認可の応答とOpenAPI仕様を検証する
- [x] 認証前のローカル所有者Resolver・サンプル初期化機能と専用テスト、`application-local.yaml`を撤去する。併せて不要になった`OwnerIdResolver`抽象を削除し、Controllerが認証主体（`@AuthenticationPrincipal Jwt`）を直接扱うよう整理する

### Section6: 認証接続後の結合テストと総仕上げ
- [x] `@SpringBootTest` + `Testcontainers` による認証からTodo操作までの一連の処理を検証する
- [x] `./gradlew test` による全テストスイートの一括実行確認
- [x] 最終コードレビューとドキュメント整理
