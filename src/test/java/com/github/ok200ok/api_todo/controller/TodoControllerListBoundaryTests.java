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
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.security.AuthenticatedUserIdResolver;
import com.github.ok200ok.api_todo.security.JwtTokenProvider;
import com.github.ok200ok.api_todo.security.SecurityConfiguration;
import com.github.ok200ok.api_todo.security.TestJwtConfiguration;
import com.github.ok200ok.api_todo.service.TodoService;

/**
 * Todo一覧APIのページ指定境界を検証するWeb層テスト
 * 認証済み要求のうえで不正なクエリをServiceへ渡さず、範囲外の有効ページは空ページとして返すことを確認する
 */
@WebMvcTest
@Import({
		SecurityConfiguration.class,
		AuthenticatedUserIdResolver.class,
		JwtTokenProvider.class,
		TestJwtConfiguration.class
})
class TodoControllerListBoundaryTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private TodoService todoService;

	/**
	 * ページ番号・サイズの不正値や数値でない値を400で拒否し、Serviceを呼ばない
	 */
	@Test
	void rejectsInvalidPageAndSizeParameters() throws Exception {
		String authorization = "Bearer " + jwtTokenProvider.createToken(UUID.randomUUID());

		mockMvc.perform(get("/api/v1/todos")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.queryParam("page", "0"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/v1/todos")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.queryParam("page", "not-a-number"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/v1/todos")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.queryParam("size", "0"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/v1/todos")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.queryParam("size", "101"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/v1/todos")
				.header(HttpHeaders.AUTHORIZATION, authorization)
				.queryParam("size", "not-a-number"))
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
		when(todoService.findTodosForOwner(ownerId, pageable)).thenReturn(emptyPage);

		mockMvc.perform(get("/api/v1/todos")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenProvider.createToken(ownerId))
				.queryParam("page", "4"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isEmpty())
				.andExpect(jsonPath("$.page").value(4))
				.andExpect(jsonPath("$.size").value(5))
				.andExpect(jsonPath("$.totalElements").value(7))
				.andExpect(jsonPath("$.totalPages").value(2));
	}
}
