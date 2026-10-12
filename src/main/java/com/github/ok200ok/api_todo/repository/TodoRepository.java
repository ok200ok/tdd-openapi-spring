package com.github.ok200ok.api_todo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.ok200ok.api_todo.entity.Todo;

public interface TodoRepository extends JpaRepository<Todo, Long> {
}
