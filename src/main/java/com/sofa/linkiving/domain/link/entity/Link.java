package com.sofa.linkiving.domain.link.entity;

import com.sofa.linkiving.domain.member.entity.Member;
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
public class Link extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(nullable = false, length = 2048)
	private String url;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String memo;

	@Column(name = "image_url", length = 2048)
	private String imageUrl;

	@Column(name = "metadata_json", columnDefinition = "TEXT")
	private String metadataJson;

	@Column(columnDefinition = "TEXT")
	private String tags;

	@Column(name = "is_important", nullable = false)
	private boolean isImportant = false;

	@Builder
	public Link(Member member, String url, String title, String memo, String imageUrl,
		String metadataJson, String tags, boolean isImportant) {
		this.member = member;
		this.url = url;
		this.title = title;
		this.memo = memo;
		this.imageUrl = imageUrl;
		this.metadataJson = metadataJson;
		this.tags = tags;
		this.isImportant = isImportant;
	}

	public static Link create(Member member, String url, String title, String memo,
		String imageUrl, String metadataJson, String tags, boolean isImportant) {
		return Link.builder()
			.member(member)
			.url(url)
			.title(title)
			.memo(memo)
			.imageUrl(imageUrl)
			.metadataJson(metadataJson)
			.tags(tags)
			.isImportant(isImportant)
			.build();
	}

	public void updateDetails(String title, String memo, String metadataJson, String tags, Boolean isImportant) {
		if (title != null) {
			this.title = title;
		}
		if (memo != null) {
			this.memo = memo;
		}
		if (metadataJson != null) {
			this.metadataJson = metadataJson;
		}
		if (tags != null) {
			this.tags = tags;
		}
		if (isImportant != null) {
			this.isImportant = isImportant;
		}
	}
}
