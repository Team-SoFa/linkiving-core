package com.sofa.linkiving.domain.link.entity;

import com.sofa.linkiving.domain.link.enums.Format;
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

	@Column(length = 64)
	private Format format;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String body;

	@Builder
	public Summary(Link link, int version, Format format, String body) {
		this.link = link;
		this.version = version;
		this.format = format;
		this.body = body;
	}
}
