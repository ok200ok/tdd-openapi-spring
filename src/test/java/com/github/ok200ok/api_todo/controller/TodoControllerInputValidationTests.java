package com.github.ok200ok.api_todo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.service.TodoService;

/**
 * MockMvcでTodo作成APIの入力境界を検証するWeb層テスト
 * {@code @WebMvcTest}はMVC関連の範囲に絞ってHTTP変換とValidationを確認し、
 * {@code @MockitoBean}でServiceを置き換えることでDBや業務ロジックから分離する
 */
@WebMvcTest
class TodoControllerInputValidationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TodoService todoService;

	@MockitoBean
	private OwnerIdResolver ownerIdResolver;

	/**
	 * タイトルの欠落、null、空白のみ、trim後の最大長超過をProblem Detailsで拒否し、
	 * trim後100文字は受け付けてServiceへ正規化済みの値を渡す
	 */
	@Test
	void validatesTitleAndAcceptsTrimmedMaximumLength() throws Exception {
		List<String> invalidRequests = List.of(
				"{\"description\":\"optional\"}",
				"{\"title\":null}",
				"{\"title\":\"   \"}",
				"{\"title\":\"" + "a".repeat(101) + "\"}");

		for (String request : invalidRequests) {
			mockMvc.perform(post("/api/v1/todos")
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
					.andExpect(status().isBadRequest())
					.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
					.andExpect(jsonPath("$.status").value(400))
					.andExpect(jsonPath("$.errors[0].field").value("title"))
					.andExpect(jsonPath("$.errors[0].message")
							.value(Matchers.matchesPattern(".*[ぁ-んァ-ン一-龯].*")));
		}
		verifyNoInteractions(todoService);

		String title = "a".repeat(100);
		Todo createdTodo = new Todo(
				new User("alice", "hashed-password", null, "ROLE_USER"),
				title,
				null,
				TodoStatusEnum.PENDING);
		ReflectionTestUtils.setField(createdTodo, "id", 42L);
		UUID ownerId = UUID.randomUUID();
		when(ownerIdResolver.resolveOwnerId()).thenReturn(ownerId);
		when(todoService.createTodo(any(UUID.class), eq(title), isNull())).thenReturn(createdTodo);

		mockMvc.perform(post("/api/v1/todos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"   " + title + "   \"}"))
				.andExpect(status().isCreated());

		verify(todoService).createTodo(eq(ownerId), eq(title), isNull());
	}

	/**
	 * 作成時の説明省略とnullを同じ入力として扱い、長い説明も受け付けてServiceへ渡す
	 */
	@Test
	void acceptsOptionalNullAndLongDescriptionOnCreate() throws Exception {
		String title = "Write tests";
		String longDescription = "x".repeat(5_000);
		UUID ownerId = UUID.randomUUID();
		Todo createdTodo = new Todo(
				new User("alice", "hashed-password", null, "ROLE_USER"),
				title,
				null,
				TodoStatusEnum.PENDING);
		ReflectionTestUtils.setField(createdTodo, "id", 42L);
		when(ownerIdResolver.resolveOwnerId()).thenReturn(ownerId);
		when(todoService.createTodo(eq(ownerId), eq(title), isNull())).thenReturn(createdTodo);
		when(todoService.createTodo(eq(ownerId), eq(title), eq(longDescription))).thenReturn(createdTodo);

		mockMvc.perform(post("/api/v1/todos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"" + title + "\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/todos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"" + title + "\",\"description\":null}"))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/todos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"" + title + "\",\"description\":\"" + longDescription + "\"}"))
				.andExpect(status().isCreated());

		verify(todoService, times(2)).createTodo(eq(ownerId), eq(title), isNull());
		verify(todoService).createTodo(eq(ownerId), eq(title), eq(longDescription));
	}

	/**
	 * 作成DTOに含まれない状態・所有者ID・任意の未知項目を400として拒否する
	 */
	@Test
	void rejectsUnknownCreateFields() throws Exception {
		List<String> invalidRequests = List.of(
				"{\"title\":\"Write tests\",\"status\":\"COMPLETED\"}",
				"{\"title\":\"Write tests\",\"userId\":\"" + UUID.randomUUID() + "\"}",
				"{\"title\":\"Write tests\",\"unexpected\":\"value\"}");

		for (String request : invalidRequests) {
			mockMvc.perform(post("/api/v1/todos")
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
					.andExpect(status().isBadRequest());
		}

		verifyNoInteractions(todoService);
	}

	/**
	 * JSON構文エラーは項目を特定できないため、項目別errorsを含めずProblem Detailsで返す
	 */
	@Test
	void returnsProblemDetailsWithoutFieldErrorsForMalformedJson() throws Exception {
		mockMvc.perform(post("/api/v1/todos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors").doesNotExist());

		verifyNoInteractions(todoService);
	}
}
