package com.github.ok200ok.api_todo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

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
