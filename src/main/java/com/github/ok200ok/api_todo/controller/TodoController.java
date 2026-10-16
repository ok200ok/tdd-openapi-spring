package com.github.ok200ok.api_todo.controller;

import java.net.URI;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.github.ok200ok.api_todo.dto.CreateTodoRequest;
import com.github.ok200ok.api_todo.dto.TodoPageResponse;
import com.github.ok200ok.api_todo.dto.TodoResponse;
import com.github.ok200ok.api_todo.dto.UpdateTodoRequest;
import com.github.ok200ok.api_todo.service.TodoService;

import jakarta.validation.Valid;

/**
 * Todo HTTP APIの入口
 * HTTP入力をDTOで受け取り、所有者解決とServiceへの委譲を行い、応答DTOでEntityを隠す
 */
// OpenAPI基本情報はSwagger UIでの識別に使い、各応答注釈はAdviceのエラー形式など推論できないHTTP契約を補う
@OpenAPIDefinition(info = @Info(
		title = "Todo API",
		version = "1.0.0",
		description = "所有者ごとのTodoを作成・参照・更新・削除するAPI"))
@Tag(name = "Todos", description = "Todoの一覧取得と個別操作")
@RestController
@RequestMapping("/api/v1/todos")
public class TodoController {

	private final TodoService todoService;
	private final OwnerIdResolver ownerIdResolver;

	public TodoController(TodoService todoService, OwnerIdResolver ownerIdResolver) {
		this.todoService = todoService;
		this.ownerIdResolver = ownerIdResolver;
	}

	@Operation(summary = "Todoを作成", description = "Todoを作成し、応答とLocationヘッダーを返す")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Todoを作成",
					headers = @Header(
							name = HttpHeaders.LOCATION, description = "作成したTodoのURL",
							schema = @Schema(type = "string")),
					content = @Content(mediaType = "application/json",
							schema = @Schema(implementation = TodoResponse.class))),
			@ApiResponse(responseCode = "400", description = "入力値が不正",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = ProblemDetail.class)))
	})
	@PostMapping
	public ResponseEntity<TodoResponse> createTodo(
			@Valid @RequestBody CreateTodoRequest request,
			UriComponentsBuilder uriBuilder) {
		var todo = todoService.createTodo(
				ownerIdResolver.resolveOwnerId(),
				request.title(),
				request.description());
		URI location = uriBuilder.path("/api/v1/todos/{todoId}")
				.buildAndExpand(todo.getId())
				.toUri();
		return ResponseEntity.created(location).body(TodoResponse.from(todo));
	}

	/**
	 * 解決済み所有者のTodoをページ単位で取得し、1始まりのページ応答として返す
	 * @param page 公開APIのページ番号。1始まりで既定値は1
	 * @param size 1ページあたりの件数。既定値は5
	 * @return Todo一覧とページ情報を含む200 OK応答
	 */
	@Operation(summary = "Todo一覧を取得", description = "所有者のTodoをページ単位で取得する")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Todo一覧を返す",
					content = @Content(mediaType = "application/json",
							schema = @Schema(implementation = TodoPageResponse.class))),
			@ApiResponse(responseCode = "400", description = "ページ指定が範囲外または数値でない",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = ProblemDetail.class)))
	})
	@GetMapping
	public ResponseEntity<TodoPageResponse> getTodos(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "5") int size) {
		if (page < 1 || size < 1 || size > 100) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ページ番号またはページサイズが範囲外です");
		}

		var todos = todoService.findTodosForOwner(
				ownerIdResolver.resolveOwnerId(),
				PageRequest.of(page - 1, size));
		return ResponseEntity.ok(TodoPageResponse.from(todos));
	}

	/**
	 * 所有者条件付きでTodoを取得し、Entityを応答DTOへ変換して返す
	 * @param todoId 取得するTodoのID
	 * @return 所有者本人のTodoを含む200 OK応答
	 */
	@Operation(summary = "Todoを取得", description = "Todo IDを指定して所有者本人のTodoを取得する")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Todoを返す",
					content = @Content(mediaType = "application/json",
							schema = @Schema(implementation = TodoResponse.class))),
			@ApiResponse(responseCode = "404", description = "Todoが存在しないか、所有者が異なる",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = ProblemDetail.class)))
	})
	@GetMapping("/{todoId}")
	public ResponseEntity<TodoResponse> getTodo(@PathVariable Long todoId) {
		var todo = todoService.findTodoByIdForOwner(todoId, ownerIdResolver.resolveOwnerId());
		return ResponseEntity.ok(TodoResponse.from(todo));
	}

	/**
	 * 所有者条件付きでTodoの削除をServiceへ委譲し、本文なしの成功応答を返す
	 * @param todoId 削除するTodoのID
	 * @return 本文を持たない204 No Content応答
	 */
	@Operation(summary = "Todoを削除", description = "Todo IDを指定して所有者本人のTodoを削除する")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Todoを削除"),
			@ApiResponse(responseCode = "404", description = "Todoが存在しないか、所有者が異なる",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = ProblemDetail.class)))
	})
	@DeleteMapping("/{todoId}")
	public ResponseEntity<Void> deleteTodo(@PathVariable Long todoId) {
		todoService.deleteTodoForOwner(todoId, ownerIdResolver.resolveOwnerId());
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Todoを更新", description = "Todo IDを指定してタイトル・説明・状態を置き換える")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Todoを更新",
					content = @Content(mediaType = "application/json",
							schema = @Schema(implementation = TodoResponse.class))),
			@ApiResponse(responseCode = "400", description = "入力値が不正",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "404", description = "Todoが存在しないか、所有者が異なる",
					content = @Content(mediaType = "application/problem+json",
							schema = @Schema(implementation = ProblemDetail.class)))
	})
	@PutMapping("/{todoId}")
	public ResponseEntity<TodoResponse> updateTodo(
			@PathVariable Long todoId,
			@Valid @RequestBody UpdateTodoRequest request) {
		var todo = todoService.updateTodoForOwner(
				todoId,
				ownerIdResolver.resolveOwnerId(),
				request.title(),
				request.description(),
				request.status());
		return ResponseEntity.ok(TodoResponse.from(todo));
	}
}
