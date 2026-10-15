package com.github.ok200ok.api_todo.entity;

import java.time.Instant;

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

/**
 * {@code todos}テーブルに対応し、JPAが永続化・取得するTodoの状態を表すEntity
 * Entityはテーブル構造とのマッピングを担い、所有者認可や初期状態などの業務ルールはService/API層が担う
 *
 * {@code @Entity}はこのクラスをJPAの管理対象にし、EntityManagerやRepositoryから永続化・検索できるようにする
 * {@code @Table}の{@code name}は対応テーブル名、{@code indexes}はテーブルに作る索引を指定する。ここではtodosテーブルとし、所有者ごとの検索を効率化するためuser_idに索引を作る。索引は外部キー制約そのものではない
 * Hibernateの{@code @Check}はDB CHECK制約を作成し、status列に定義済みの状態名だけを許可する。アプリケーション外からの書き込みにもDB側の制約を適用する
 */
@Entity
@Table(name = "todos", indexes = @Index(name = "idx_todos_user_id", columnList = "user_id"))
@Check(name = "ck_todos_status", constraints = "status in ('PENDING', 'IN_PROGRESS', 'COMPLETED')")
public class Todo {

	/**
	 * Todoを一意に識別する主キー
	 * {@code @Id}はこのプロパティをJPAの主キーとして指定する
	 * {@code @GeneratedValue(strategy = GenerationType.IDENTITY)}はPostgreSQLのIDENTITY列にINSERT時の採番を任せる。ID値をINSERT前に決められないため、永続化時にINSERTが必要となり、生成値がJPAからEntityへ反映される
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * このTodoの所有者を表すUserへの多対一関連
	 * 多数のTodoが同じUserに属する関係で、Todoの読み書き権限はこの関連だけでは保証されずService/API層で認可する
	 * {@code fetch = LAZY}はTodo取得時にUser本体をすぐ取得せず、関連へアクセスした時点で読み込む指定
	 * 不要な関連データ取得を避ける一方、永続化コンテキスト終了後のアクセスでは遅延ロードできない場合がある
	 * {@code optional = false}はこの関連が必須であることをJPAに示す。{@code @JoinColumn(name = "user_id", nullable = false)}は関連先の主キーを格納する非NULL外部キー列を指定する
	 * 削除カスケードは指定していないため、参照中Userの削除は外部キー制約で拒否される
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/**
	 * Todoの必須タイトルをVARCHAR(100)として保存するカラム
	 * {@code @Column(nullable = false, length = 100)}はDB列をNOT NULLかつ最大100文字として定義する。これはHTTP入力のBean Validationではなく永続化スキーマの指定
	 */
	@Column(nullable = false, length = 100)
	private String title;

	/**
	 * 任意の詳細説明を保存するカラム
	 * {@code columnDefinition = "text"}はHibernateが生成するDDLでPostgreSQLのTEXT型を明示し、VARCHARの固定長上限を設けない
	 * DB固有の型名に依存する指定なので、異なるDBへ移行する場合はスキーマ定義の見直しが必要
	 */
	@Column(columnDefinition = "text")
	private String description;

	/**
	 * Todoの業務状態を表すEnum値
	 * {@code @Enumerated(EnumType.STRING)}はEnum定数名を文字列で保存する指定。宣言順の数値を保存するORDINAL方式では、Enumの並べ替え・追加で既存データの意味が変わり得るため文字列を選ぶ
	 * {@code @Column(nullable = false, length = 32)}は状態列を必須のVARCHAR(32)として定義し、クラスのCHECK制約がDB側で許可する状態名を限定する
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private TodoStatusEnum status;

	/**
	 * Todoが作成された時点をUTC基準の{@code Instant}で保存する必須カラム
	 * PostgreSQLでは{@code timestamptz}に対応付け、{@code @PrePersist}で設定して更新時には変更しない
	 */
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	/**
	 * Todoの作成・最終更新時点をUTC基準の{@code Instant}で保存する必須カラム
	 * PostgreSQLでは{@code timestamptz}に対応付ける
	 * {@code @PrePersist}で初期化し、{@code @PreUpdate}でEntityの変更をDBへ反映する際に更新する
	 */
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Todo() {
	}

	public Todo(User user, String title, String description, TodoStatusEnum status) {
		this.user = user;
		this.title = title;
		this.description = description;
		this.status = status;
	}

	/**
	 * Entityを初めて永続化する直前にJPAから呼び出される
	 * DBのNOT NULL制約を満たすため、INSERT前に作成時点と更新時点をUTC基準で初期化する
	 */
	@PrePersist
	void beforeInsert() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	/**
	 * 永続化済みEntityの変更をDBへ反映する直前にJPAから呼び出される
	 * 作成時点は保持し、更新時点だけをUTC基準の現在時刻へ進める
	 */
	@PreUpdate
	void beforeUpdate() {
		updatedAt = Instant.now();
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

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
