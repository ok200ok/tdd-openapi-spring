package com.github.ok200ok.api_todo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spring BootアプリケーションとTestcontainers上のPostgreSQLを結合して確認するテスト
 * DB接続やアプリケーションコンテキストの起動など、個別のレイヤーコンポーネントを越えた基盤の動作を検証する
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ApiTodoApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void postgresContainerIsReachable() {
		String databaseVersion = jdbcTemplate.queryForObject("SELECT version()", String.class);

		assertThat(databaseVersion).contains("PostgreSQL");
	}

}
