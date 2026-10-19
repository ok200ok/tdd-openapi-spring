package com.github.ok200ok.api_todo.dto;

/**
 * ユーザー登録APIの入力契約
 * HTTP入力をEntityから分離し、Controllerで受け取る項目を定義する
 */
public record RegisterRequest(String userName, String password, String displayName) {
}
