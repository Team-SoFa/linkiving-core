package com.sofa.linkiving.security.jwt.error;

import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.Getter;

@Getter
public class CustomJwtException extends BusinessException {
	public CustomJwtException(JwtErrorCode errorCode) {
		super(errorCode);
	}
}
