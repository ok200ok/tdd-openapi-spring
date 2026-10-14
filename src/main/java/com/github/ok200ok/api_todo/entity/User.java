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

/**
 * usersテーブルに対応し、JPAが永続化・取得するUserの状態を表すEntity
 * ユーザー名の正規化と作成・更新時刻の設定は永続化ライフサイクルに合わせて行い、形式・一意性はDB制約でも保証する
 */
@Entity
// テーブル名とuser_nameの一意制約を定義し、正規化後のログイン名重複をDBでも防ぐ
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_username", columnNames = "user_name"))
// アプリケーションの外から直接DBへ書き込まれた場合にも小文字英数字以外を拒否するCHECK制約
@Check(name = "ck_users_username_lower_alphanumeric", constraints = "user_name ~ '^[a-z0-9]+$'")
public class User {

	// 主キーとして扱い、UUID生成をJPAプロバイダーに委ねる
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	// DB列名をuser_nameに固定し、NOT NULLとVARCHAR(50)のスキーマ制約を定義する
	@Column(name = "user_name", nullable = false, length = 50)
	private String userName;

	// ハッシュ化済みパスワードを最大255文字で保存する。nullable=falseは値の検証ではなくDB列をNOT NULLにする指定
	@Column(nullable = false, length = 255)
	private String password;

	// 表示名は省略可能なのでnullableの既定値trueを使い、最大50文字の列に対応付ける
	@Column(name = "display_name", length = 50)
	private String displayName;

	// Roleは必須のVARCHAR(50)として保存する
	@Column(nullable = false, length = 50)
	private String role;

	// 作成時に設定し、以後の更新では値を変えないタイムスタンプ
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	// 新規作成時と更新時にEntityのライフサイクルコールバックで設定するタイムスタンプ
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

	/**
	 * Entityが初めて永続化される直前にJPAから呼び出されるコールバック
	 * INSERT前にログイン名を正規化し、作成時刻と更新時刻を同じ現在時刻で初期化する
	 */
	@PrePersist
	void beforeInsert() {
		canonicalizeUserName();
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	/**
	 * 永続化済みEntityの変更をDBへ反映する直前にJPAから呼び出されるコールバック
	 * 更新時刻を進め、ログイン名も正規化する。SQL発行のたびではなくEntity更新時に実行される
	 */
	@PreUpdate
	void beforeUpdate() {
		canonicalizeUserName();
		updatedAt = LocalDateTime.now();
	}

	private void canonicalizeUserName() {
		if (userName != null) {
			// Locale.ROOTでロケール依存の大小文字変換を避け、ログイン名の正規化を実行環境間で一定にする
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
