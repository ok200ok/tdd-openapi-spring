package com.github.ok200ok.api_todo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.ok200ok.api_todo.dto.RegisterRequest;
import com.github.ok200ok.api_todo.dto.RegisterResponse;
import com.github.ok200ok.api_todo.service.AuthService;

import jakarta.validation.Valid;

/**
 * 認証APIのHTTP入口
 * 登録・ログインの入力をDTOで受け取り、AuthServiceへ委譲して応答DTOへ変換する
 * この段階では登録テストをコンパイルして機能上のREDを確認するための骨格
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

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
	@PostMapping("/register")
	public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
		throw new UnsupportedOperationException("registerは未実装です");
	}
}
