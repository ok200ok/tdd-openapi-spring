package com.github.ok200ok.api_todo.service;

import java.util.UUID;

/**
 * 指定されたIDのUserが存在しない場合にServiceが通知する例外
 * 呼び出し元が所有者不在を他の失敗と区別できるようにする
 */
public class UserNotFoundException extends RuntimeException {

	public UserNotFoundException(UUID userId) {
		super("User not found: " + userId);
	}
}
