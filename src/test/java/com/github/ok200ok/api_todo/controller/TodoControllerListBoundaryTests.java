package com.github.ok200ok.api_todo.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.service.TodoService;

/**
 * Todo一覧APIのページ指定境界を検証するWeb層テスト
 * 不正なクエリをServiceへ渡さず、範囲外の有効ページは空ページとして返すことを確認する
 */
@WebMvcTest
class TodoControllerListBoundaryTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TodoService todoService;

	@MockitoBean
	private OwnerIdResolver ownerIdResolver;

	/**
	 * ページ番号・サイズの不正値や数値でない値を400で拒否し、Serviceを呼ばない
	 */
	@Test
	void rejectsInvalidPageAndSizeParameters() throws Exception {
		mockMvc.perform(get("/api/v1/todos").queryParam("page", "0"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/v1/todos").queryParam("page", "not-a-number"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/v1/todos").queryParam("size", "0"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/v1/todos").queryParam("size", "101"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/v1/todos").queryParam("size", "not-a-number"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(todoService);
	}

	/**
	 * 範囲外の有効ページは空のitemsと実データに基づくページ情報を返す
	 */
	@Test
	void returnsEmptyPageBeyondLastPageWithActualTotals() throws Exception {
		UUID ownerId = UUID.randomUUID();
		PageRequest pageable = PageRequest.of(3, 5);
		Page<Todo> emptyPage = new PageImpl<>(List.of(), pageable, 7);
		when(ownerIdResolver.resolveOwnerId()).thenReturn(ownerId);
		when(todoService.findTodosForOwner(ownerId, pageable)).thenReturn(emptyPage);

		mockMvc.perform(get("/api/v1/todos").queryParam("page", "4"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isEmpty())
				.andExpect(jsonPath("$.page").value(4))
				.andExpect(jsonPath("$.size").value(5))
				.andExpect(jsonPath("$.totalElements").value(7))
				.andExpect(jsonPath("$.totalPages").value(2));
	}
}
