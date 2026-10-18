package com.github.ok200ok.api_todo.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.ok200ok.api_todo.entity.User;
import com.github.ok200ok.api_todo.repository.UserRepository;

/**
 * ユーザー登録などの業務処理を担うServiceレイヤーコンポーネント
 * パスワードを保存前にハッシュ化し、登録時のroleをサーバー側で決定する
 */
@Service
public class UserService {

	private static final String DEFAULT_ROLE = "ROLE_USER";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * ユーザー名が未使用であることを確認して、新しい一般ユーザーを登録する
	 *
	 * @param userName ログインに使用するユーザー名
	 * @param rawPassword ハッシュ化前のパスワード
	 * @param displayName 任意の表示名
	 * @return Repositoryが保存したUser
	 * @throws UserAlreadyExistsException 同じユーザー名が既に登録されている場合
	 */
	@Transactional
	public User registerUser(String userName, String rawPassword, String displayName) {
		if (userRepository.findByUserName(userName).isPresent()) {
			throw new UserAlreadyExistsException(userName);
		}

		String encodedPassword = passwordEncoder.encode(rawPassword);
		User user = new User(userName, encodedPassword, displayName, DEFAULT_ROLE);
		return userRepository.save(user);
	}
}
