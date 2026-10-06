package com.github._okmk.api_todo;

import org.springframework.boot.SpringApplication;

public class TestApiTodoApplication {

	public static void main(String[] args) {
		SpringApplication.from(ApiTodoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
