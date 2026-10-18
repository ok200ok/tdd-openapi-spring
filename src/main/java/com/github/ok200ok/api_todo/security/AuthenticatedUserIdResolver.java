package com.github.ok200ok.api_todo.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.github.ok200ok.api_todo.controller.OwnerIdResolver;

/**
 * SecurityContextの認証済みJWTからTodo所有者のUser UUIDを解決する
 * 既存Controllerの所有者解決境界を保ちつつ、ローカル固定ユーザーを認証主体へ置き換える
 */
@Component
public class AuthenticatedUserIdResolver implements OwnerIdResolver {

	/**
	 * Resource Serverが設定した認証主体のJWT subjectをUser UUIDとして返す
	 *
	 * @return 認証済みUserのUUID
	 * @throws IllegalStateException SecurityContextにJWT認証情報がない場合
	 * @throws IllegalArgumentException subjectがUUID形式でない場合
	 */
	@Override
	public UUID resolveOwnerId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
			throw new IllegalStateException("認証済みJWTがSecurityContextにありません");
		}

		return UUID.fromString(jwtAuthentication.getToken().getSubject());
	}
}
