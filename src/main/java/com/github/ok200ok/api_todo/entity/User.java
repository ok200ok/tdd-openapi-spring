package com.github.ok200ok.api_todo.entity;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.annotations.Check;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_username", columnNames = "user_name"))
@Check(name = "ck_users_username_lower_alphanumeric", constraints = "user_name ~ '^[a-z0-9]+$'")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_name", nullable = false, length = 50)
	private String userName;

	@Column(nullable = false, length = 255)
	private String password;

	@Column(name = "display_name", length = 50)
	private String displayName;

	@Column(nullable = false, length = 50)
	private String role;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected User() {
	}

	public User(String userName, String password, String displayName, String role) {
		this.userName = userName;
		this.password = password;
		this.displayName = displayName;
		this.role = role;
	}

	@PrePersist
	void beforeInsert() {
		canonicalizeUserName();
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void beforeUpdate() {
		canonicalizeUserName();
		updatedAt = LocalDateTime.now();
	}

	private void canonicalizeUserName() {
		if (userName != null) {
			userName = userName.toLowerCase(Locale.ROOT);
		}
	}

	public void changeDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public UUID getId() {
		return id;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getRole() {
		return role;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

}
