package com.sofa.linkiving.global.config;

import org.springdoc.core.customizers.ParameterCustomizer;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sofa.linkiving.global.config.annotation.DecodeHash;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openApi(ObjectProvider<BuildProperties> buildProperties) {
		String version = buildProperties.stream()
			.findFirst()
			.map(BuildProperties::getVersion)
			.orElse("0.1.0");

		return new OpenAPI()
			.info(new Info()
				.title("Linkiving API")
				.version(version));
	}

	@Bean
	public ParameterCustomizer hashidParameterCustomizer() {
		return (parameterModel, methodParameter) -> {
			if (methodParameter.hasParameterAnnotation(DecodeHash.class)) {
				parameterModel.setSchema(new StringSchema());
				parameterModel.setExample("aB9x2K8R");
				String currentDesc = parameterModel.getDescription() != null ? parameterModel.getDescription() : "";
				if (!currentDesc.contains("해시")) {
					parameterModel.setDescription(currentDesc + " (해시 문자열)");
				}
			}
			return parameterModel;
		};
	}

	@Bean
	public PropertyCustomizer hashidPropertyCustomizer() {
		return (property, type) -> {
			if (type.getCtxAnnotations() == null) {
				return property;
			}

			boolean isHashidField = false;

			for (java.lang.annotation.Annotation annotation : type.getCtxAnnotations()) {

				if (annotation instanceof JsonSerialize serialize) {
					if (serialize.using().getSimpleName().contains("Hashids")) {
						isHashidField = true;
						break;
					}
				} else if (annotation instanceof JsonDeserialize deserialize) {
					if (deserialize.using().getSimpleName().contains("Hashids")) {
						isHashidField = true;
						break;
					}
				}
			}

			if (isHashidField) {
				property.setType("string");
				property.setFormat(null);
				property.setExample("aB9x2K8R");

				String desc = property.getDescription() != null ? property.getDescription() : "";
				if (!desc.contains("해시")) {
					property.setDescription(desc + " (해시된 문자열)");
				}
			}

			return property;
		};
	}
}
