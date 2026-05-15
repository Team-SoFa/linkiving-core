package com.sofa.linkiving.global.config.web;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.sofa.linkiving.global.config.annotation.DecodeHash;
import com.sofa.linkiving.global.util.HashidsUtils;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DecodeHashInterceptor implements HandlerInterceptor {

	private final HashidsUtils hashidsUtils;

	@Override
	public boolean preHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response,
		@Nonnull Object handler) {
		if (!(handler instanceof HandlerMethod handlerMethod)) {
			return true;
		}

		Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		Map<String, String> pathVars = new HashMap<>();

		if (attribute instanceof Map<?, ?> map) {
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (entry.getKey() instanceof String key && entry.getValue() instanceof String value) {
					pathVars.put(key, value);
				}
			}
		}

		Method specificMethod = ClassUtils.getMostSpecificMethod(handlerMethod.getMethod(),
			handlerMethod.getBeanType());

		for (Parameter param : specificMethod.getParameters()) {

			if (param.isAnnotationPresent(DecodeHash.class)) {
				String rawValue = null;

				if (param.isAnnotationPresent(PathVariable.class)) {
					PathVariable pv = param.getAnnotation(PathVariable.class);
					String name = StringUtils.hasText(pv.value()) ? pv.value() : pv.name();
					if (!StringUtils.hasText(name)) {
						name = param.getName();
					}

					if (name != null) {
						rawValue = pathVars.get(name);
					}
				} else if (param.isAnnotationPresent(RequestParam.class)) {
					RequestParam rp = param.getAnnotation(RequestParam.class);
					String name = StringUtils.hasText(rp.value()) ? rp.value() : rp.name();
					if (!StringUtils.hasText(name)) {
						name = param.getName();
					}

					if (name != null) {
						rawValue = request.getParameter(name);
					}
				}

				if (StringUtils.hasText(rawValue)) {
					hashidsUtils.decode(rawValue);
				}
			}
		}
		return true;
	}
}
