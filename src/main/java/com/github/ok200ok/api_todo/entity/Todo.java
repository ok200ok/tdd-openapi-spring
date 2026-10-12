package com.github.ok200ok.api_todo.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Check;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "todos", indexes = @Index(name = "idx_todos_user_id", columnList = "user_id"))
@Check(name = "ck_todos_status", constraints = "status in ('PENDING', 'IN_PROGRESS', 'COMPLETED')")
public class Todo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private TodoStatusEnum status;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Todo() {
	}

	public Todo(User user, String title, String description, TodoStatusEnum status) {
		this.user = user;
		this.title = title;
		this.description = description;
		this.status = status;
	}

	@PrePersist
	void beforeInsert() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void beforeUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public void changeDetails(String title, String description) {
		this.title = title;
		this.description = description;
	}

	public void changeStatus(TodoStatusEnum status) {
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public TodoStatusEnum getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

}
