package com.github.ok200ok.api_todo.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.ok200ok.api_todo.service.TodoService;

/**
 * Todo削除APIのHTTP契約とService連携を検証するWeb層テスト
 * Serviceと所有者ID解決境界をモックし、削除後の応答をHTTP境界で確認する
 */
@WebMvcTest
class TodoControllerDeleteTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TodoService todoService;

	@MockitoBean
	private OwnerIdResolver ownerIdResolver;

	/**
	 * 削除成功時に解決済み所有者IDとTodo IDでServiceを呼び、本文のない204を返す
	 */
	@Test
	void deletesTodoForOwnerAndReturnsNoContent() throws Exception {
		UUID ownerId = UUID.randomUUID();
		when(ownerIdResolver.resolveOwnerId()).thenReturn(ownerId);

		mockMvc.perform(delete("/api/v1/todos/42"))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		verify(todoService).deleteTodoForOwner(42L, ownerId);
	}
}
