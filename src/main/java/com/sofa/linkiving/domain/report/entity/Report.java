package com.sofa.linkiving.domain.report.entity;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	Member member;

	@Column(nullable = false, columnDefinition = "TEXT")
	String content;

	@Builder
	public Report(Member member, String content) {
		this.member = member;
		this.content = content;
	}
}
