package com.github.ok200ok.api_todo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.ok200ok.api_todo.entity.Todo;

/**
* Repositoryはデータアクセスの窓口です。JpaRepository を継承することで、保存・検索・削除といった基本操作が利用でき、Spring Data JPAが実装を提供します。
* TodoRepository
 */
public interface TodoRepository extends JpaRepository<Todo, Long> {
}
