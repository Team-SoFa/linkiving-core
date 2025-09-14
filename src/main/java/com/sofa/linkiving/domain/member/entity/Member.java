package com.sofa.linkiving.domain.member.entity;

import com.sofa.linkiving.global.common.BaseEntity;

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

	@Column(nullable = false)
	private String email;
	@Column(nullable = false)
	private String password;

	@Builder
	public Member(String email, String password) {
		this.email = email;
		this.password = password;
	}
}
