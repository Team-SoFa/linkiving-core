package com.sofa.linkiving.domain.link.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.sofa.linkiving.domain.link.enums.Format;
import com.sofa.linkiving.domain.member.entity.Member;

public class SummaryTest {

	@Test
	void shouldCreateSummaryWithRequiredFields() {
		// given
		Member member = Member.builder()
			.email("test@test.com")
			.password("password")
			.build();

		Link link = Link.builder()
			.member(member)
			.url("https://example.com")
			.title("Test Title")
			.build();

		Format format = Format.DETAILED;
		String content = "This is a summary";

		//when
		Summary summary = Summary.builder()
			.link(link)
			.format(format)
			.content(content)
			.build();

		//then
		assertThat(summary.getLink()).isEqualTo(link);
		assertThat(summary.getFormat()).isEqualTo(format);
		assertThat(summary.getContent()).isEqualTo(content);
	}

	@Test
	void shouldCreateSummaryWithAllFields() {
		//given
		Member member = Member.builder()
			.email("test@test.com")
			.password("password")
			.build();

		Link link = Link.builder()
			.member(member)
			.url("https://example.com")
			.title("Test Title")
			.build();

		Format format = Format.DETAILED;
		String content = "This is a detailed summary";
		String select = "Selected text from the page";

		Summary summary = Summary.builder()
			.link(link)
			.format(format)
			.content(content)
			.select(select)
			.build();

		assertThat(summary.getLink()).isEqualTo(link);
		assertThat(summary.getContent()).isEqualTo(content);
		assertThat(summary.getFormat()).isEqualTo(format);
		assertThat(summary.getSelected()).isEqualTo(select);
	}
}
