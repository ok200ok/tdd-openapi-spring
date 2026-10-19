# Section 5 タスク6 引き継ぎメモ

このファイルは、新しいセッションで「登録・ログインAPIとOpenAPI」を完了するための現在地と次の手順を短時間で把握するためのメモ。仕様の正は次の文書とし、このメモでは現在地と次に必要な判断だけを補う。

- [認証・認可設計仕様](../authentication-design.md)
- [DTO・Validation設計仕様](../dto-validation.md)
- [API設計仕様](../api-design.md)
- [テストケース一覧](../test-case-catalog.md)

過去の長い対話履歴はGit履歴を参照する。

## 現在地

- ブランチ: `feature/springsec-jwtauth`
- 2026-10-19時点: `./gradlew test` で98件成功
- 登録API、認証基盤、既存Controllerテストの整備は完了済み
- 残りは`AuthControllerLoginTests`の3ケース、ログインController実装、OpenAPI反映
- Section 5タスク6のロードマップは未完了のまま

## 完了済み

- `JpaUserDetailsService`: `UserRepository`からUserを取得し、保存済みハッシュとroleを`UserDetails`へ変換
- `AuthenticationManagerConfiguration`: `DaoAuthenticationProvider`と`ProviderManager`を構成し、ユーザー不存在を`BadCredentialsException`へ隠蔽
- `AuthService.register`: `UserService`で登録後、User UUIDでJWTを発行
- `AuthService.login`: `AuthenticationManager`で認証後、`Authentication.getName()`でUserを再取得し、UUIDでJWTを発行
- `AuthController.register`: `201 Created`、`RegisterResponse`返却、409ハンドラ
- `RegisterRequest`: `userName`・`password`・任意`displayName`の検証
- `Utf8ByteLength`: パスワードのUTF-8 72バイト上限を検証
- 既存のTodo系・セキュリティ系10クラスを`@WebMvcTest(TodoController.class)`へ限定
- `OpenApiDocumentationTests`へ`@MockitoBean AuthService`を追加
- `ApiExceptionHandler`: 入力エラー400、未知フィールド・不正JSON400、Todo404、ユーザー名重複409
- `SecurityConfiguration`: `/api/v1/auth/**`を明示的に`permitAll`

## 残作業

### 1. `AuthControllerLoginTests`の3ケース

`@WebMvcTest(AuthController.class)`、`@Import({SecurityConfiguration.class, TestJwtConfiguration.class})`、`@MockitoBean AuthService`で検証する。

- `logsInAndReturnsToken`
  - `200 OK`
  - `TokenResponse`の`accessToken`、`tokenType`（`Bearer`）、`expiresIn`（900）
  - ユーザー情報、`password`、`role`、`id`を返さない
  - `AuthService.login(userName, password)`へ入力を渡す
- `returnsUnauthorizedWhenCredentialsAreInvalid`
  - `AuthService.login`が`BadCredentialsException`を送出
  - `401 Unauthorized`、`application/problem+json`、`status: 401`
  - `detail`: `ユーザー名またはパスワードが正しくありません`
  - `errors`拡張なし
  - `WWW-Authenticate`ヘッダーなし
- `rejectsInvalidLoginInput`
  - 登録と同じ`userName`・`password`制約
  - `400`、`application/problem+json`、該当`field`を`errors`に含める
  - `AuthService`を呼ばない
  - 登録の`rejectsInvalidRegistrationInput`と同じ入力から`displayName`を除く

### 2. ログインController実装

- `LoginRequest`を追加
  - `userName`: `@NotBlank`、`@Size(max = 50)`、`@Pattern(regexp = "^[a-z0-9]+$")`
  - `password`: `@NotBlank`、`@Size(min = 8)`、`@Utf8ByteLength(max = 72)`
- `TokenResponse`を追加
  - `accessToken`、`tokenType`、`expiresIn`
- `AuthController.login`
  - `POST /api/v1/auth/login`
  - `@Valid @RequestBody LoginRequest`
  - `authService.login(...)`の結果を`TokenResponse`へ変換
  - `200 OK`
- `ApiExceptionHandler`へ`BadCredentialsException`の401変換を追加
  - `ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "ユーザー名またはパスワードが正しくありません")`
  - `WWW-Authenticate`は付けない
  - `AuthenticationException`全体ではなく`BadCredentialsException`だけを対象にする

### 3. OpenAPI反映

- `OpenApiConfiguration`を`com.github.ok200ok.api_todo.config`へ新規作成
  - `@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")`
- `TodoController`へクラスレベルの`@SecurityRequirement(name = "bearerAuth")`を追加
- `AuthController`へ`@Tag`、`@Operation`、`@ApiResponses`
  - register: `201`、`400`、`409`
  - login: `200`、`400`、`401`
- 認証APIには`@SecurityRequirement`を付けない
- `OpenApiDocumentationTests`へ`@Import(OpenApiConfiguration.class)`を追加
- `OpenApiDocumentationTests`へ`publishesAuthApiOperationsSchemasAndBearerSecurityScheme`を追加
  - auth pathsとstatus
  - `RegisterRequest`・`LoginRequest`・`RegisterResponse`・`TokenResponse`スキーマ
  - 応答スキーマに`password`・`role`・`id`がないこと
  - `components.securitySchemes.bearerAuth`の`type=http`、`scheme=bearer`、`bearerFormat=JWT`
  - Todo操作に`security`、認証APIに`security`がないこと

## 次のTDD手順

1. `AuthControllerLoginTests`のテストコードだけを作成する。テストは`AuthService.LoginResult`だけを参照するため、`AuthController.login`・`LoginRequest`・`TokenResponse`は未作成のままREDを確認して停止する
2. 開発者のRED確認後、`AuthController.login`、401ハンドラ、DTOを最小実装し、GREENを確認する
3. カタログのログイン3ケースを✅へ更新する
4. `OpenApiConfiguration`は`OpenApiDocumentationTests`が`@Import`で参照するため、空の骨格だけ先に置いてテストをコンパイル可能にし、`@SecurityScheme`未定義のREDを確認する。その後、設定・注釈を実装してGREENを確認する
5. OpenAPIケースを✅へ更新し、READMEのタスク6を完了にする
6. `./gradlew test`で全件成功を確認する

## 注意

- `AuthenticationManagerConfiguration`と`AuthService.login`は実装済み。作り直さない
- 既存Todoテストの`@WebMvcTest(TodoController.class)`を元へ戻さない
- `OpenApiDocumentationTests`は全Controllerを読み込むため、`AuthService`のモックを外さない
- 実際の`DaoAuthenticationProvider`を通したログインHTTPフローはまだ未検証。Section 6の結合テストで確認する
- カタログのリンクはテストコード変更後に行番号を再確認して更新する

## 主なファイル

- `src/main/java/com/github/ok200ok/api_todo/controller/AuthController.java`
- `src/main/java/com/github/ok200ok/api_todo/controller/ApiExceptionHandler.java`
- `src/main/java/com/github/ok200ok/api_todo/controller/TodoController.java`
- `src/main/java/com/github/ok200ok/api_todo/dto/RegisterRequest.java`
- `src/main/java/com/github/ok200ok/api_todo/dto/RegisterResponse.java`
- `src/main/java/com/github/ok200ok/api_todo/service/AuthService.java`
- `src/main/java/com/github/ok200ok/api_todo/config/AuthenticationManagerConfiguration.java`
- `src/main/java/com/github/ok200ok/api_todo/security/SecurityConfiguration.java`
- `src/test/java/com/github/ok200ok/api_todo/controller/AuthControllerRegisterTests.java`
- `src/test/java/com/github/ok200ok/api_todo/controller/OpenApiDocumentationTests.java`
- `src/test/java/com/github/ok200ok/api_todo/security/TestJwtConfiguration.java`

## 検証コマンド

```sh
# ログインController
./gradlew test --tests 'com.github.ok200ok.api_todo.controller.AuthControllerLoginTests'

# OpenAPI
./gradlew test --tests 'com.github.ok200ok.api_todo.controller.OpenApiDocumentationTests'

# 全体
./gradlew test
```
