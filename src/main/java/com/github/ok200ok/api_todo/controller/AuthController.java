package com.github.ok200ok.api_todo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.ok200ok.api_todo.dto.LoginRequest;
import com.github.ok200ok.api_todo.dto.RegisterRequest;
import com.github.ok200ok.api_todo.dto.RegisterResponse;
import com.github.ok200ok.api_todo.dto.TokenResponse;
import com.github.ok200ok.api_todo.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 認証APIのHTTP入口
 * 登録・ログインの入力をDTOで受け取り、AuthServiceへ委譲して応答DTOへ変換する
 * 認証手順やEntityの内部情報は公開せず、API契約として定義した項目だけを返す
 */
@Tag(name = "Authentication", description = "ユーザー登録とログイン")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private static final String TOKEN_TYPE = "Bearer";

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/**
	 * ユーザー登録要求を受け取り、登録結果とaccess tokenを返す
	 *
	 * @param request 登録するユーザー名・パスワード・任意の表示名
	 * @return 登録されたユーザーの公開情報とaccess tokenを含む201 Created
	 */
	@Operation(summary = "ユーザー登録", description = "ユーザーを登録し、access tokenを返す")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "ユーザーを登録",
					content = @Content(mediaType = "application/json",
							schema = @Schema(implementation = RegisterResponse.class))),
			@ApiResponse(responseCode = "400", description = "入力値が不正",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "409", description = "ユーザー名が既に登録されている",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = ProblemDetail.class)))
	})
	@PostMapping("/register")
	public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
		AuthService.RegistrationResult result = authService.register(
				request.userName(), request.password(), request.displayName());
		RegisterResponse response = new RegisterResponse(
				result.user().getUserName(),
				result.user().getDisplayName(),
				result.accessToken(),
				TOKEN_TYPE,
				result.expiresInSeconds());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * ログイン要求を受け取り、認証成功時のaccess tokenを返す
	 *
	 * @param request ログインに使用するユーザー名・パスワード
	 * @return access tokenと有効期間を含む200 OK
	 */
	@Operation(summary = "ログイン", description = "資格情報を検証し、access tokenを返す")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "ログイン成功",
					content = @Content(mediaType = "application/json",
							schema = @Schema(implementation = TokenResponse.class))),
			@ApiResponse(responseCode = "400", description = "入力値が不正",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "401", description = "ユーザー名またはパスワードが正しくない",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = ProblemDetail.class)))
	})
	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		// 資格情報の検証はAuthServiceへ委譲し、ControllerはHTTP入出力の変換だけを担う
		AuthService.LoginResult result = authService.login(
				request.userName(), request.password());
		TokenResponse response = new TokenResponse(
				result.accessToken(),
				TOKEN_TYPE,
				result.expiresInSeconds());
		return ResponseEntity.ok(response);
	}
}
