package com.sofa.linkiving.global.config.web;

import java.text.ParseException;
import java.util.Locale;
import java.util.Set;

import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.stereotype.Component;

import com.sofa.linkiving.global.config.annotation.DecodeHash;
import com.sofa.linkiving.global.util.HashidsUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HashidsFormatterFactory implements AnnotationFormatterFactory<DecodeHash> {

	private final HashidsUtils hashidsUtils;

	@Override
	public Set<Class<?>> getFieldTypes() {
		return Set.of(Long.class);
	}

	@Override
	public Parser<?> getParser(DecodeHash annotation, Class<?> fieldType) {
		return new Parser<Long>() {
			@Override
			public Long parse(String text, Locale locale) throws ParseException {
				return hashidsUtils.decode(text);
			}
		};
	}

	@Override
	public Printer<?> getPrinter(DecodeHash annotation, Class<?> fieldType) {
		return new Printer<Long>() {
			@Override
			public String print(Long object, Locale locale) {
				return hashidsUtils.encode(object);
			}
		};
	}
}
