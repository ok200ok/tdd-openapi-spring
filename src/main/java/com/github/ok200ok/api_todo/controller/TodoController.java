package com.github.ok200ok.api_todo.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.ok200ok.api_todo.dto.CreateTodoRequest;
import com.github.ok200ok.api_todo.dto.TodoResponse;
import com.github.ok200ok.api_todo.dto.UpdateTodoRequest;
import com.github.ok200ok.api_todo.service.TodoService;

import jakarta.validation.Valid;

/**
 * Todo HTTP APIの入口
 * HTTP入力をDTOで受け取り、所有者解決とServiceへの委譲を行い、応答DTOでEntityを隠す
 */
@RestController
@RequestMapping("/api/v1/todos")
public class TodoController {

	private final TodoService todoService;
	private final OwnerIdResolver ownerIdResolver;

	public TodoController(TodoService todoService, OwnerIdResolver ownerIdResolver) {
		this.todoService = todoService;
		this.ownerIdResolver = ownerIdResolver;
	}

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
