package com.github.ok200ok.api_todo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;

import com.github.ok200ok.api_todo.service.TodoNotFoundException;
import com.github.ok200ok.api_todo.service.UserAlreadyExistsException;

/**
 * Controller境界で発生する入力エラーや未検出例外をProblem Detailsへ変換する共通処理
 * 各Controllerに同じHTTPエラー形式の組み立てを重複させない
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidationError(MethodArgumentNotValidException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "入力内容を確認してください");
		List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> Map.of(
						"field", error.getField(),
						"message", error.getDefaultMessage() == null ? "入力値が正しくありません" : error.getDefaultMessage()))
				.toList();
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleUnreadableRequest(HttpMessageNotReadableException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.BAD_REQUEST, "リクエスト本文を読み取れません");
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(TodoNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleTodoNotFound(TodoNotFoundException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.NOT_FOUND, "指定されたTodoが見つかりません");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}

	/**
	 * ユーザー名の一意制約違反を409 ConflictのProblem Detailsへ変換する
	 * 認証情報の重複は入力形式の誤りではないため400ではなく409とし、errors拡張は付けない
	 */
	@ExceptionHandler(UserAlreadyExistsException.class)
	public ResponseEntity<ProblemDetail> handleUserAlreadyExists(UserAlreadyExistsException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.CONFLICT, "指定されたユーザー名は既に登録されています");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
	}

	/**
	 * ログイン資格情報の不一致を401 UnauthorizedのProblem Detailsへ変換する
	 * ユーザー不存在とパスワード不一致を区別せず、クライアントには同じ理由だけを返す
	 * 認証方式を問い合わせるBearerチャレンジはログインAPIには不要なため、WWW-Authenticateを付けない
	 * AuthenticationException全体ではなく、DaoAuthenticationProviderが返すBadCredentialsExceptionだけを対象にする
	 */
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.UNAUTHORIZED, "ユーザー名またはパスワードが正しくありません");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
	}
}
