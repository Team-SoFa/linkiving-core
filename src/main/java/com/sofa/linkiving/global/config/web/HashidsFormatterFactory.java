package com.sofa.linkiving.global.config.web;

import java.util.Set;

import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.global.config.annotation.DecodeHash;
import com.sofa.linkiving.global.util.HashidsUtils;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HashidsFormatterFactory implements AnnotationFormatterFactory<DecodeHash> {

	private final HashidsUtils hashidsUtils;

	@Nonnull
	@Override
	public Set<Class<?>> getFieldTypes() {
		return Set.of(Long.class);
	}

	@Nonnull
	@Override
	public Parser<?> getParser(@Nonnull DecodeHash annotation, @Nonnull Class<?> fieldType) {
		return (Parser<Long>)(text, locale) -> hashidsUtils.decode(text);
	}

	@Nonnull
	@Override
	public Printer<?> getPrinter(@Nonnull DecodeHash annotation, @Nonnull Class<?> fieldType) {
		return (Printer<Long>)(object, locale) -> hashidsUtils.encode(object);
	}
}
