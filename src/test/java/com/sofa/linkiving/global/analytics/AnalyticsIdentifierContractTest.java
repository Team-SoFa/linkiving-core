package com.sofa.linkiving.global.analytics;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofa.linkiving.domain.link.analytics.SummaryAnalyticsPublisher;
import com.sofa.linkiving.domain.link.dto.response.LinkCardRes;
import com.sofa.linkiving.domain.link.dto.response.LinkDetailRes;
import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.member.dto.response.MemberProfileRes;
import com.sofa.linkiving.global.config.jackson.HashidsSerializer;
import com.sofa.linkiving.global.util.HashidsUtils;

class AnalyticsIdentifierContractTest {

	private HashidsUtils previousHashids;

	private final ObjectMapper mapper = new ObjectMapper();
	private final HashidsUtils hashids = new HashidsUtils("analytics-contract-test", 8);

	@BeforeEach
	void setUp() {
		previousHashids = (HashidsUtils)ReflectionTestUtils.getField(HashidsSerializer.class, "hashidsUtils");
		new HashidsSerializer().setHashidsUtils(hashids);
	}

	@AfterEach
	void tearDown() {
		ReflectionTestUtils.setField(HashidsSerializer.class, "hashidsUtils", previousHashids);
	}

	@Test
	void responsesKeepRouteIdsAndExposeDecimalAnalyticsIds() throws Exception {
		List<Object> responses = List.of(
			MemberProfileRes.builder().id(605L).build(),
			new LinkCardRes(605L, null, null, null, null, null),
			new LinkDetailRes(605L, null, null, null, null, null, null),
			new LinkRes(605L, null, null, null, null, null)
		);
		for (Object response : responses) {
			JsonNode json = mapper.readTree(mapper.writeValueAsString(response));
			assertThat(json.get("id").asText()).isEqualTo(hashids.encode(605L));
			assertThat(json.get("analyticsId").isTextual()).isTrue();
			assertThat(json.get("analyticsId").asText()).isEqualTo("605");
		}
	}

	@Test
	void responseIdentifiersMatchServerEventIdentifiers() throws Exception {
		Ga4MeasurementProtocolClient client = mock(Ga4MeasurementProtocolClient.class);
		Ga4Publisher publisher = new Ga4Publisher(client);
		new SummaryAnalyticsPublisher(publisher).publishComplete(
			AnalyticsContext.of("test-client", "extension"), 42L, 605L, false, System.nanoTime());
		ArgumentCaptor<Ga4Event> event = ArgumentCaptor.forClass(Ga4Event.class);
		verify(client).send(eq("test-client"), eq(MemberProfileRes.builder().id(42L).build().analyticsId()),
			event.capture());
		assertThat(event.getValue().params().get("link_id").toString())
			.isEqualTo(new LinkCardRes(605L, null, null, null, null, null).analyticsId());
	}
}
