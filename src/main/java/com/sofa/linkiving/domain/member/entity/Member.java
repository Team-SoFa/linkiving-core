package com.sofa.linkiving.domain.member.entity;

import com.sofa.linkiving.global.common.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {
	@NotBlank
	private String email;
	@NotBlank
	private String password;

	/**
	 * Constructs a Member with the given email and password.
	 *
	 * <p>Intended for use with Lombok's {@code @Builder}. Both fields are subject to
	 * bean-validation {@code @NotBlank} constraints when validated.</p>
	 *
	 * @param email    the member's email address (must be non-blank)
	 * @param password the member's password (must be non-blank)
	 */
	@Builder
	public Member(String email, String password) {
		this.email = email;
		this.password = password;
	}
}
