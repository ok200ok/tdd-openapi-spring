package com.github.ok200ok.api_todo.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.ok200ok.api_todo.entity.User;

/**
 * Userの永続化と検索を担うRepositoryレイヤーコンポーネント
 * {@code JpaRepository<User, UUID>}によりUserとUUID主キーの汎用CRUD操作が提供され、個別検索はクエリメソッドとして宣言する
 */
public interface UserRepository extends JpaRepository<User, UUID> {

	/**
	 * userNameが一致するUserを検索するクエリメソッド
	 * {@code findBy}に続くプロパティ名からSpring Data JPAが検索条件を導出し、未検出をnullではなくOptional.emptyで表す
	 *
	 * @param userName 検索するログイン名
	 * @return 一致するUser。存在しない場合はOptional.empty
	 */
	Optional<User> findByUserName(String userName);

}
