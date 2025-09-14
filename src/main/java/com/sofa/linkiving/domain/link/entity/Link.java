package com.sofa.linkiving.domain.link.entity;

import com.sofa.linkiving.global.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "links")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Link extends BaseEntity {

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "url", nullable = false, length = 2048)
	private String url;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "summary", columnDefinition = "TEXT")
	private String summary;

	@Column(name = "memo", columnDefinition = "TEXT")
	private String memo;

	@Column(name = "image_url", length = 2048)
	private String imageUrl;

	@Builder
	public Link(Long memberId, String url, String title, String summary, String memo, String imageUrl) {
		this.memberId = memberId;
		this.url = url;
		this.title = title;
		this.summary = summary;
		this.memo = memo;
		this.imageUrl = imageUrl;
	}
}

