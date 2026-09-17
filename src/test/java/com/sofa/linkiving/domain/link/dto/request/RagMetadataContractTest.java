package com.sofa.linkiving.domain.link.dto.request;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;
import com.sofa.linkiving.domain.member.entity.Member;

class RagMetadataContractTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void allRequestsCarryTheSameOriginalKoreanSavedTime() {
		Link link = link();
		ReflectionTestUtils.setField(link, "createdAt", LocalDateTime.of(2026, 8, 17, 0, 30));
		ReflectionTestUtils.setField(link, "updatedAt", LocalDateTime.of(2026, 9, 17, 12, 0));
		link.updateDetails("수정된 제목", "수정된 메모", null);
		link.updateSummaryStatus(SummaryStatus.COMPLETED);

		for (Object request : requests(link)) {
			JsonNode json = mapper.valueToTree(request);
			assertThat(json.path("savedAt").asText()).isEqualTo("2026-08-17T00:30+09:00");
			assertThat(json.path("savedAtEpochMs").asLong())
				.isEqualTo(Instant.parse("2026-08-16T15:30:00Z").toEpochMilli());
			assertThat(json.path("summaryStatus").asText()).isEqualTo("COMPLETED");
			assertThat(json.path("linkId").asLong()).isEqualTo(123L);
			assertThat(json.path("userId").asLong()).isEqualTo(45L);
			assertThat(json.path("url").asText()).isEqualTo("https://example.org/article");
		}
	}

	@Test
	void missingSavedTimeIsNotReplacedByNowOrModifiedTime() {
		Link link = link();
		ReflectionTestUtils.setField(link, "updatedAt", LocalDateTime.of(2026, 9, 17, 12, 0));
		for (Object request : requests(link)) {
			JsonNode json = mapper.valueToTree(request);
			assertThat(json.has("savedAt")).isFalse();
			assertThat(json.has("savedAtEpochMs")).isFalse();
			assertThat(json.path("summaryStatus").asText()).isEqualTo("PENDING");
		}
	}

	@Test
	void defaultJvmTimezoneDoesNotChangeTheStoredTimeInterpretation() {
		TimeZone previous = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
			Link link = link();
			ReflectionTestUtils.setField(link, "createdAt", LocalDateTime.of(2026, 8, 17, 0, 30));
			assertThat(RagInitialSummaryReq.from(link).savedAtEpochMs())
				.isEqualTo(Instant.parse("2026-08-16T15:30:00Z").toEpochMilli());
		} finally {
			TimeZone.setDefault(previous);
		}
	}

	@Test
	void retriesAndRegenerationKeepOriginalTimeAndCurrentStatus() {
		Link link = link();
		ReflectionTestUtils.setField(link, "createdAt", LocalDateTime.of(2026, 8, 17, 0, 30));
		Long original = RagInitialSummaryReq.from(link).savedAtEpochMs();
		link.updateSummaryStatus(SummaryStatus.FAILED);
		assertThat(LinkSyncUpdateReq.from(link).summaryStatus()).isEqualTo(SummaryStatus.FAILED);
		link.updateSummaryStatus(SummaryStatus.COMPLETED);
		assertThat(RagRegenerateSummaryReq.of(link, "이전 요약").savedAtEpochMs()).isEqualTo(original);
		assertThat(RagRegenerateSummaryReq.of(link, "이전 요약").summary()).isEqualTo("이전 요약");
	}

	private List<Object> requests(Link link) {
		return List.of(RagInitialSummaryReq.from(link), RagRegenerateSummaryReq.of(link, "기존 요약"),
			LinkSyncUpdateReq.from(link), LinkSyncUpdateReq.of(link, null));
	}

	private Link link() {
		Member member = Member.builder().email("test@example.org").build();
		ReflectionTestUtils.setField(member, "id", 45L);
		Link link = Link.create(member, "https://example.org/article", "제목", "메모", null);
		ReflectionTestUtils.setField(link, "id", 123L);
		return link;
	}
}
