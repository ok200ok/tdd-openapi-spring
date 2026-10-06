package com.github.ok200ok.api_todo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ApiTodoApplicationTests {

	@Test
	void contextLoads() {
	}

}
