package com.sofa.linkiving.domain.link.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.sofa.linkiving.domain.member.entity.Member;

public class LinkTest {

	@Test
	void shouldCreateLinkWithRequiredFields() {
		Member member = Member.builder()
			.email("test@test.com")
			.password("password")
			.build();
		String url = "https://example.com";
		String title = "Example Title";

		Link link = Link.builder()
			.member(member)
			.url(url)
			.title(title)
			.build();

		assertThat(link.getMember()).isEqualTo(member);
		assertThat(link.getUrl()).isEqualTo(url);
		assertThat(link.getTitle()).isEqualTo(title);
		assertThat(link.isImportant()).isFalse();
	}

	@Test
	void shouldCreateLinkWithAllFields() {
		Member member = Member.builder()
			.email("test@test.com")
			.password("password")
			.build();
		String url = "https://example.com";
		String title = "Example Title";
		String memo = "Test memo";
		String imageUrl = "https://example.com/image.jpg";
		String metadataJson = "{\"key\":\"value\"}";
		String tags = "[\"tag1\",\"tag2\"]";
		boolean isImportant = true;

		Link link = Link.builder()
			.member(member)
			.url(url)
			.title(title)
			.memo(memo)
			.imageUrl(imageUrl)
			.metadataJson(metadataJson)
			.tags(tags)
			.isImportant(isImportant)
			.build();

		assertThat(link.getMember()).isEqualTo(member);
		assertThat(link.getUrl()).isEqualTo(url);
		assertThat(link.getTitle()).isEqualTo(title);
		assertThat(link.getMemo()).isEqualTo(memo);
		assertThat(link.getImageUrl()).isEqualTo(imageUrl);
		assertThat(link.getMetadataJson()).isEqualTo(metadataJson);
		assertThat(link.getTags()).isEqualTo(tags);
		assertThat(link.isImportant()).isTrue();
	}
}
