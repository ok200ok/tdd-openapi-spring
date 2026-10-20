package com.github.ok200ok.api_todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Todo APIアプリケーションの起動クラス
 * {@code @SpringBootApplication}によりコンポーネントスキャンと自動設定を有効化し、組み込みサーバーを起動する
 */
@SpringBootApplication
public class ApiTodoApplication {

	/**
	 * アプリケーションを起動するエントリーポイント
	 * @param args 起動時に渡されるコマンドライン引数
	 */
	public static void main(String[] args) {
		SpringApplication.run(ApiTodoApplication.class, args);
	}

}
