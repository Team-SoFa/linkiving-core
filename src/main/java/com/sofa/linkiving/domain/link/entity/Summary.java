package com.sofa.linkiving.domain.link.entity;

import com.sofa.linkiving.global.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Summary extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "link_id", nullable = false)
	private Link link;

	@Column(nullable = false)
	private int version;

	@Column(name = "summary_format", nullable = false, length = 64)
	private String summaryFormat;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String body;

	@Column(name = "token_count")
	private Integer tokenCount;

	@Column(name = "created_by", length = 255)
	private String createdBy;

	@Column(nullable = false, length = 64)
	private String status;

	@Builder
	public Summary(Link link, int version, String summaryFormat, String body,
		Integer tokenCount, String createdBy, String status) {
		this.link = link;
		this.version = version;
		this.summaryFormat = summaryFormat;
		this.body = body;
		this.tokenCount = tokenCount;
		this.createdBy = createdBy;
		this.status = status;
	}
}
