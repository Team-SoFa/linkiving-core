package com.sofa.linkiving.domain.link.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.sofa.linkiving.domain.member.entity.Member;

public class SummaryTest {

	@Test
	void shouldCreateSummaryWithRequiredFields() {
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
		String summaryFormat = "concise";
		String body = "This is a summary";
		String status = "completed";

		Summary summary = Summary.builder()
			.link(link)
			.version(version)
			.summaryFormat(summaryFormat)
			.body(body)
			.status(status)
			.build();

		assertThat(summary.getLink()).isEqualTo(link);
		assertThat(summary.getVersion()).isEqualTo(version);
		assertThat(summary.getSummaryFormat()).isEqualTo(summaryFormat);
		assertThat(summary.getBody()).isEqualTo(body);
		assertThat(summary.getStatus()).isEqualTo(status);
	}

	@Test
	void shouldCreateSummaryWithAllFields() {
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
		String summaryFormat = "detail";
		String body = "This is a detailed summary";
		Integer tokenCount = 150;
		String createdBy = "AI";
		String status = "completed";

		Summary summary = Summary.builder()
			.link(link)
			.version(version)
			.summaryFormat(summaryFormat)
			.body(body)
			.tokenCount(tokenCount)
			.createdBy(createdBy)
			.status(status)
			.build();

		assertThat(summary.getLink()).isEqualTo(link);
		assertThat(summary.getVersion()).isEqualTo(version);
		assertThat(summary.getSummaryFormat()).isEqualTo(summaryFormat);
		assertThat(summary.getBody()).isEqualTo(body);
		assertThat(summary.getTokenCount()).isEqualTo(tokenCount);
		assertThat(summary.getCreatedBy()).isEqualTo(createdBy);
		assertThat(summary.getStatus()).isEqualTo(status);
	}
}
