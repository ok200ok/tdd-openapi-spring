package com.github.ok200ok.api_todo.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 文字列がUTF-8で指定バイト数以内であることを検証する制約
 * Javaの文字数ではなく、DBや認証ライブラリが実際に扱うバイト長をController境界で確認する
 */
@Documented
@Constraint(validatedBy = Utf8ByteLengthValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Utf8ByteLength {

	String message() default "UTF-8で{max}バイト以内で入力してください";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	int max();
}
