package com.github.ok200ok.api_todo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL TestcontainersをSpringのテストコンテキストへ登録する共有設定
 * Repositoryテストと結合テストが同じPostgreSQLメジャーバージョンを使い、開発用DBから分離して検証できるようにする
 * {@code @TestConfiguration}は通常のコンポーネントスキャンでは取り込まれないテスト専用設定を定義する。このトップレベル設定を使うテストは{@code @Import}で明示的に読み込む
 * {@code proxyBeanMethods = false}は@Beanメソッド間の直接呼び出しをSpringが代理して同一Beanへ置き換える機能を無効にする。この設定ではBeanメソッド間の呼び出しがないため、代理を省略できる
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	/**
	 * {@code @Bean}は戻り値をSpringコンテナが管理するBeanとして登録し、コンテナの起動・終了をテストコンテキストのライフサイクルに結び付ける
	 * {@code @ServiceConnection}はTestcontainersから接続先URLや認証情報を取得してSpring Bootのデータソース設定へ渡し、接続設定の手書きを不要にする
	 * {@code postgres:17-alpine}はPostgreSQL 17のAlpine Linux版イメージを指定し、開発用DBと同じPostgreSQLメジャーバージョンで検証する
	 */
	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
	}

}
