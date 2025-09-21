package com.sofa.linkiving.domain.link.entity;

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
@Table(name = "links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Link extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

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
	public Link(Member member, String url, String title, String summary, String memo, String imageUrl) {
		this.member = member;
		this.url = url;
		this.title = title;
		this.summary = summary;
		this.memo = memo;
		this.imageUrl = imageUrl;
	}
}

