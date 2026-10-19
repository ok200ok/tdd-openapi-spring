package com.github.ok200ok.api_todo.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.github.ok200ok.api_todo.repository.UserRepository;

/**
 * UserRepositoryからUserを取得し、Spring SecurityのUserDetailsへ変換するUserDetailsService実装
 * この段階ではテストをコンパイルして機能上のREDを確認するための骨格で、認証ロジックはGREENで実装する
 */
public class JpaUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public JpaUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		throw new UnsupportedOperationException("JpaUserDetailsServiceは未実装です");
	}
}
