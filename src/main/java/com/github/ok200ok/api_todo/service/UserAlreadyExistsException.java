package com.github.ok200ok.api_todo.service;

/**
 * 登録しようとしたユーザー名が既に使用されている場合にUserServiceが通知する例外
 * 呼び出し側が重複登録を他の失敗と区別し、登録APIの応答へ変換できる
 */
public class UserAlreadyExistsException extends RuntimeException {

	public UserAlreadyExistsException(String userName) {
		super("User name is already registered: " + userName);
	}
}
