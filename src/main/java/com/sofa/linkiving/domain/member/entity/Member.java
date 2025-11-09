package com.sofa.linkiving.domain.member.entity;

import java.util.regex.Pattern;

import com.sofa.linkiving.domain.member.enums.Role;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.global.common.BaseEntity;
import com.sofa.linkiving.global.error.exception.BusinessException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {
	private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z]{2,6}$";
	private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE);

	@Column(nullable = false, unique = true)
	private String email;
	@Column(nullable = false)
	private String password;
	@Column(nullable = false)
	private Role role;

	@Builder
	public Member(String email, String password) {
		if (!isValidEmail(email)) {
			throw new BusinessException(MemberErrorCode.INVALID_EMAIL_FORMAT);
		}
		this.email = email;
		this.password = password;
		this.role = Role.USER;
	}

	private boolean isValidEmail(String email) {
		return EMAIL_PATTERN.matcher(email).matches();
	}

	public boolean verifyPassword(String rawPassword) {
		return this.password.equals(rawPassword);
	}
}
