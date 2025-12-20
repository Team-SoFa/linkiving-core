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

	@Column(length = 64)
	private Format format;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	@Column(name = "selected")
	private boolean selected;

	@Builder
	public Summary(Link link, Format format, String content, boolean select) {
		this.link = link;
		this.format = format;
		this.content = content;
		this.selected = select;
	}
}
