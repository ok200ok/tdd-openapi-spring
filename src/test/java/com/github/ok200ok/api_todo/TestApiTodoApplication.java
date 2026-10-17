package com.github.ok200ok.api_todo;

import org.springframework.boot.SpringApplication;

/**
 * 結合テスト用にTestcontainersの設定を加えてアプリケーションを起動するエントリーポイント
 * このクラスから起動すると、通常のアプリケーション設定にテスト専用Beanを追加してコンテキストを構成できる
 */
public class TestApiTodoApplication {

	public static void main(String[] args) {
		SpringApplication.from(ApiTodoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
