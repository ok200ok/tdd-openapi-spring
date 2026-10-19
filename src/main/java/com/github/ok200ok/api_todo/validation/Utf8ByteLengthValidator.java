package com.github.ok200ok.api_todo.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Utf8ByteLengthの検証実装
 * この段階ではテストをコンパイルして機能上のREDを確認するための骨格
 */
public class Utf8ByteLengthValidator implements ConstraintValidator<Utf8ByteLength, CharSequence> {

	private int max;

	@Override
	public void initialize(Utf8ByteLength annotation) {
		this.max = annotation.max();
	}

	@Override
	public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
		throw new UnsupportedOperationException("UTF-8バイト長検証は未実装です");
	}
}
