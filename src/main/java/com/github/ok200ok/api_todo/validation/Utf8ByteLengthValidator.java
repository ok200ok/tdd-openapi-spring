package com.github.ok200ok.api_todo.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Utf8ByteLengthの検証実装
 * Javaの文字数ではなく、UTF-8へ変換した実際のバイト長を上限と比較する
 */
public class Utf8ByteLengthValidator implements ConstraintValidator<Utf8ByteLength, CharSequence> {

	private int max;

	@Override
	public void initialize(Utf8ByteLength annotation) {
		this.max = annotation.max();
	}

	@Override
	public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
		// nullは@NotBlankなど必須チェックの責務に委ね、この制約では違反にしない
		if (value == null) {
			return true;
		}
		return value.toString().getBytes(StandardCharsets.UTF_8).length <= max;
	}
}
