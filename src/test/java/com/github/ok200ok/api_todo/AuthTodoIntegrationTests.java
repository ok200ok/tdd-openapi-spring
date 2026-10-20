package com.github.ok200ok.api_todo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.github.ok200ok.api_todo.dto.CreateTodoRequest;
import com.github.ok200ok.api_todo.dto.LoginRequest;
import com.github.ok200ok.api_todo.dto.RegisterRequest;
import com.github.ok200ok.api_todo.dto.RegisterResponse;
import com.github.ok200ok.api_todo.dto.TodoPageResponse;
import com.github.ok200ok.api_todo.dto.TodoResponse;
import com.github.ok200ok.api_todo.dto.TokenResponse;
import com.github.ok200ok.api_todo.dto.UpdateTodoRequest;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.repository.UserRepository;

/**
 * 認証からTodo操作までの一連の処理を実HTTPサーバー・PostgreSQL・JWTで検証する結合テスト
 *
 * {@code webEnvironment = RANDOM_PORT}は埋め込みサーバーを空きポートで起動し、MockMvcでは省略されるSecurityFilterChain、Bearer認証、JSON変換、トランザクションを実際のHTTP経路で動かす
 * {@code @AutoConfigureRestTestClient}は起動中サーバーへ接続するRestTestClientをテストコンテキストへ登録する
 * {@code @Import(TestcontainersConfiguration.class)}は開発用DBとは独立したTestcontainersのPostgreSQLをデータソースとして接続する
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class AuthTodoIntegrationTests {

	@Autowired
	private RestTestClient restTestClient;

	// ハッシュ化して保存されたことをHTTP応答だけでは確認できないため、永続状態をRepositoryで直接照合する
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	/**
	 * 登録・ログイン・Todoの作成・一覧・取得・更新・削除を順に実行し、各HTTP契約とDB永続化が接続して動作することを確認する
	 * 既存実装の検証のため新規本番コードは伴わず、実サーバー経由で一連の処理が成立することをGREENの根拠とする
	 */
	@Test
	void completesRegisterLoginAndTodoCrudFlow() {
		String userName = uniqueUserName("alice");
		String password = "password123";

		RegisterResponse registration = register(userName, password);
		assertThat(registration.accessToken()).isNotBlank();
		assertThat(registration.tokenType()).isEqualTo("Bearer");
		assertThat(registration.expiresIn()).isEqualTo(900);

		User storedUser = userRepository.findByUserName(userName).orElseThrow();
		assertThat(storedUser.getPassword())
				.isNotEqualTo(password)
				.startsWith("$2");
		assertThat(passwordEncoder.matches(password, storedUser.getPassword())).isTrue();

		TokenResponse login = login(userName, password);
		assertThat(login.accessToken()).isNotBlank();
		assertThat(login.tokenType()).isEqualTo("Bearer");
		assertThat(login.expiresIn()).isEqualTo(900);

		String token = login.accessToken();
		EntityExchangeResult<TodoResponse> createdResult = restTestClient.post()
				.uri("/api/v1/todos")
				.headers(headers -> headers.setBearerAuth(token))
				.contentType(MediaType.APPLICATION_JSON)
				.body(new CreateTodoRequest("Read documentation", "Spring Security"))
				.exchange()
				.expectStatus().isCreated()
				.expectBody(TodoResponse.class)
				.returnResult();
		TodoResponse created = createdResult.getResponseBody();
		URI location = createdResult.getResponseHeaders().getLocation();

		assertThat(created).isNotNull();
		assertThat(created.id()).isNotNull();
		assertThat(created.title()).isEqualTo("Read documentation");
		assertThat(created.description()).isEqualTo("Spring Security");
		assertThat(created.status()).isEqualTo(TodoStatusEnum.PENDING);
		assertThat(created.createdAt()).isNotNull();
		assertThat(created.updatedAt()).isNotNull();
		assertThat(location).isNotNull();
		assertThat(location.getPath()).isEqualTo("/api/v1/todos/" + created.id());

		TodoPageResponse page = restTestClient.get()
				.uri("/api/v1/todos?page=1&size=5")
				.headers(headers -> headers.setBearerAuth(token))
				.exchange()
				.expectStatus().isOk()
				.expectBody(TodoPageResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(page).isNotNull();
		assertThat(page.page()).isEqualTo(1);
		assertThat(page.size()).isEqualTo(5);
		assertThat(page.totalElements()).isEqualTo(1);
		assertThat(page.totalPages()).isEqualTo(1);
		assertThat(page.items()).extracting(TodoResponse::id).containsExactly(created.id());

		TodoResponse fetched = restTestClient.get()
				.uri("/api/v1/todos/{todoId}", created.id())
				.headers(headers -> headers.setBearerAuth(token))
				.exchange()
				.expectStatus().isOk()
				.expectBody(TodoResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(fetched).isNotNull();
		assertThat(fetched.id()).isEqualTo(created.id());
		assertThat(fetched.title()).isEqualTo(created.title());
		assertThat(fetched.status()).isEqualTo(TodoStatusEnum.PENDING);

		TodoResponse updated = restTestClient.put()
				.uri("/api/v1/todos/{todoId}", created.id())
				.headers(headers -> headers.setBearerAuth(token))
				.contentType(MediaType.APPLICATION_JSON)
				.body(new UpdateTodoRequest(
						"Read documentation thoroughly", "Spring Security and JPA", TodoStatusEnum.COMPLETED))
				.exchange()
				.expectStatus().isOk()
				.expectBody(TodoResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(updated).isNotNull();
		assertThat(updated.title()).isEqualTo("Read documentation thoroughly");
		assertThat(updated.description()).isEqualTo("Spring Security and JPA");
		assertThat(updated.status()).isEqualTo(TodoStatusEnum.COMPLETED);
		assertThat(updated.createdAt()).isEqualTo(created.createdAt());
		assertThat(updated.updatedAt()).isAfterOrEqualTo(created.updatedAt());

		restTestClient.delete()
				.uri("/api/v1/todos/{todoId}", created.id())
				.headers(headers -> headers.setBearerAuth(token))
				.exchange()
				.expectStatus().isNoContent();

		restTestClient.get()
				.uri("/api/v1/todos/{todoId}", created.id())
				.headers(headers -> headers.setBearerAuth(token))
				.exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentTypeCompatibleWith("application/problem+json")
				.expectBody()
				.jsonPath("$.status").isEqualTo(404)
				.jsonPath("$.detail").isEqualTo("指定されたTodoが見つかりません");
	}

	private RegisterResponse register(String userName, String password) {
		RegisterResponse response = restTestClient.post()
				.uri("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.body(new RegisterRequest(userName, password, "Alice"))
				.exchange()
				.expectStatus().isCreated()
				.expectBody(RegisterResponse.class)
				.returnResult()
				.getResponseBody();
		assertThat(response).isNotNull();
		return response;
	}

	private TokenResponse login(String userName, String password) {
		TokenResponse response = restTestClient.post()
				.uri("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.body(new LoginRequest(userName, password))
				.exchange()
				.expectStatus().isOk()
				.expectBody(TokenResponse.class)
				.returnResult()
				.getResponseBody();
		assertThat(response).isNotNull();
		return response;
	}

	// テスト実行順に依存せず、他テストのユーザーと衝突しない小文字英数字のユーザー名を作る
	private String uniqueUserName(String prefix) {
		return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
	}

}
