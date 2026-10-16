package com.github.ok200ok.api_todo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.github.ok200ok.api_todo.entity.Todo;
import com.github.ok200ok.api_todo.entity.TodoStatusEnum;
import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.repository.TodoRepository;
import com.github.ok200ok.api_todo.repository.UserRepository;

/**
 * localプロファイル起動時にデモユーザーのTodoをサンプルデータへ戻す初期化処理
 * 認証追加前のローカル学習用途に限定し、他ユーザーのデータには触れない
 */
@Component
@Profile("local")
public class LocalSampleDataInitializer implements ApplicationRunner {

	private static final String LOCAL_ONLY_PASSWORD = "LOCAL_ONLY_NOT_A_LOGIN_PASSWORD";
	private static final List<String> SAMPLE_TITLES = List.of(
			"Write project README",
			"Review API design",
			"Add repository tests",
			"Implement Todo creation",
			"Validate request DTO",
			"Check pagination behavior",
			"Document error responses",
			"Review controller tests",
			"Try the local API",
			"Inspect generated SQL",
			"Update sample tasks",
			"Check timestamp handling",
			"Review ownership rules",
			"Run the full test suite",
			"Prepare authentication design");

	private final UserRepository userRepository;
	private final TodoRepository todoRepository;
	private final String ownerUsername;

	public LocalSampleDataInitializer(
			UserRepository userRepository,
			TodoRepository todoRepository,
			@Value("${app.local-owner.username}") String ownerUsername) {
		this.userRepository = userRepository;
		this.todoRepository = todoRepository;
		this.ownerUsername = ownerUsername;
	}

	/**
	 * Spring Bootの起動完了時にローカル用初期データを一括適用する
	 * 削除と再作成を同じトランザクションにまとめ、途中失敗でサンプルデータが消失した状態を残さない
	 * @param args 起動引数。初期化処理では参照しない
	 */
	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		initialize();
	}

	/**
	 * デモユーザーを取得または作成し、そのユーザーのTodoだけをサンプル15件へ置き換える
	 */
	public void initialize() {
		User owner = findOrCreateOwner();
		List<Todo> existingTodos = todoRepository.findAllByUser_IdOrderByUpdatedAtDescIdDesc(owner.getId());
		if (!existingTodos.isEmpty()) {
			todoRepository.deleteAll(existingTodos);
		}

		List<Todo> sampleTodos = SAMPLE_TITLES.stream()
				.map(title -> new Todo(owner, title, null, TodoStatusEnum.PENDING))
				.toList();
		todoRepository.saveAll(sampleTodos);
	}

	private User findOrCreateOwner() {
		Optional<User> existingOwner = userRepository.findByUserName(ownerUsername);
		if (existingOwner.isPresent()) {
			return existingOwner.get();
		}

		User owner = new User(ownerUsername, LOCAL_ONLY_PASSWORD, "Local demo", "ROLE_USER");
		return userRepository.save(owner);
	}
}
