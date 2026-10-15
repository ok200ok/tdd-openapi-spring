package com.github.ok200ok.api_todo.controller;

import java.util.UUID;

/**
 * リクエストを処理する所有者のIDを解決する境界
 * Controllerをローカル用・認証用のID取得方法から分離し、それぞれの導入段階で解決方法を差し替えられる
 */
public interface OwnerIdResolver {

	UUID resolveOwnerId();
}
