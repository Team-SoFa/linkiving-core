package com.sofa.linkiving.domain.user.entity;

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
public class User extends BaseEntity {
	@NotBlank
	private String email;
	@NotBlank
	private String password;

	@Builder
	public User(String email, String password) {
		this.email = email;
		this.password = password;
	}
}
