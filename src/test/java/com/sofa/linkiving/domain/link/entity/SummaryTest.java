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

		int version = 1;
		Format format = Format.DETAILED;
		String body = "This is a summary";

		//when
		Summary summary = Summary.builder()
			.link(link)
			.version(version)
			.format(format)
			.body(body)
			.build();

		//then
		assertThat(summary.getLink()).isEqualTo(link);
		assertThat(summary.getVersion()).isEqualTo(version);
		assertThat(summary.getFormat()).isEqualTo(format);
		assertThat(summary.getBody()).isEqualTo(body);
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

		int version = 2;
		Format format = Format.DETAILED;
		String body = "This is a detailed summary";

		Summary summary = Summary.builder()
			.link(link)
			.version(version)
			.format(format)
			.body(body)
			.build();

		assertThat(summary.getLink()).isEqualTo(link);
		assertThat(summary.getVersion()).isEqualTo(version);
		assertThat(summary.getBody()).isEqualTo(body);
		assertThat(summary.getFormat()).isEqualTo(format);
	}
}
