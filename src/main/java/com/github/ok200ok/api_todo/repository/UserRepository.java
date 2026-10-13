package com.github.ok200ok.api_todo.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.ok200ok.api_todo.entity.User;

/**
* Repositoryはデータアクセスの窓口であり「どう永続化・検索するか」を担う。JpaRepository を継承することで、保存・検索・削除といった基本操作が利用でき、Spring Data JPAが実装を提供します。
* UserRepository
 */
public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByUserName(String userName);

}
