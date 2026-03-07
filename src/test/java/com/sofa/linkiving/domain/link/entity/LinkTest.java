package com.sofa.linkiving.domain.link.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sofa.linkiving.domain.link.enums.SummaryStatus;
import com.sofa.linkiving.domain.link.error.SummaryErrorCode;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

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

		Link link = Link.builder()
			.member(member)
			.url(url)
			.title(title)
			.memo(memo)
			.imageUrl(imageUrl)
			.build();

		assertThat(link.getMember()).isEqualTo(member);
		assertThat(link.getUrl()).isEqualTo(url);
		assertThat(link.getTitle()).isEqualTo(title);
		assertThat(link.getMemo()).isEqualTo(memo);
		assertThat(link.getImageUrl()).isEqualTo(imageUrl);
	}

	@Test
	@DisplayName("상태 값을 성공적으로 변경함")
	void shouldUpdateSummaryStatus() {
		// given
		Member member = Member.builder()
			.email("test@test.com")
			.password("password")
			.build();

		Link link = Link.builder()
			.member(member)
			.url("http://test.com")
			.title("테스트 제목")
			.build();

		// 기본값은 PENDING이어야 함
		assertThat(link.getSummaryStatus()).isEqualTo(SummaryStatus.PENDING);

		// when
		link.updateSummaryStatus(SummaryStatus.COMPLETED);

		// then
		assertThat(link.getSummaryStatus()).isEqualTo(SummaryStatus.COMPLETED);
	}

	@Test
	@DisplayName("상태가 PENDING이면 ALREADY_PROCESSING 예외 발생함")
	void shouldThrowException_WhenStatusIsPending() {
		// given
		Member member = Member
			.builder()
			.email("test@test.com")
			.password("password")
			.build();

		Link link = Link.builder()
			.member(member)
			.url("http://test.com")
			.title("title")
			.build();
		link.updateSummaryStatus(SummaryStatus.PENDING);

		// when & then
		assertThatThrownBy(link::validateSummarizable)
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", SummaryErrorCode.ALREADY_PROCESSING);
	}

	@Test
	@DisplayName("상태가 PROCESSING이면 ALREADY_PROCESSING 예외 발생함")
	void shouldThrowException_WhenStatusIsProcessing() {
		// given
		Member member = Member
			.builder()
			.email("test@test.com")
			.password("password")
			.build();

		Link link = Link.builder()
			.member(member)
			.url("http://test.com")
			.title("title")
			.build();

		link.updateSummaryStatus(SummaryStatus.PROCESSING);

		// when & then
		assertThatThrownBy(link::validateSummarizable)
			.isInstanceOf(BusinessException.class)
			.hasFieldOrPropertyWithValue("errorCode", SummaryErrorCode.ALREADY_PROCESSING);
	}

	@Test
	@DisplayName("상태가 COMPLETED이면 예외 없이 통과함")
	void shouldPassValidation_WhenStatusIsCompleted() {
		// given
		Member member = Member.builder()
			.email("test@test.com")
			.password("password")
			.build();

		Link link = Link.builder()
			.member(member)
			.url("http://test.com")
			.title("title")
			.build();

		link.updateSummaryStatus(SummaryStatus.COMPLETED);

		// when & then
		assertThatCode(link::validateSummarizable).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("상태가 FAILED이면 예외 없이 통과함")
	void shouldPassValidation_WhenStatusIsFailed() {
		// given
		Member member = Member.builder()
			.email("test@test.com")
			.password("password")
			.build();

		Link link = Link.builder()
			.member(member)
			.url("http://test.com")
			.title("title")
			.build();

		link.updateSummaryStatus(SummaryStatus.FAILED);

		// when & then
		assertThatCode(link::validateSummarizable).doesNotThrowAnyException();
	}
}
