package com.sofa.linkiving.infra.feign;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.annotation.AnnotationAwareRetryOperationsInterceptor;
import org.springframework.retry.interceptor.Retryable;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.link.ai.LinkSyncClient;
import com.sofa.linkiving.domain.link.ai.LinkSyncFeign;
import com.sofa.linkiving.domain.link.ai.RagLinkSyncClient;
import com.sofa.linkiving.domain.link.ai.SummaryClient;
import com.sofa.linkiving.domain.link.analytics.SummaryAnalyticsPublisher;
import com.sofa.linkiving.domain.link.config.SummaryWorkerProperties;
import com.sofa.linkiving.domain.link.dto.request.LinkSyncDeleteReq;
import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.enums.SyncAction;
import com.sofa.linkiving.domain.link.event.LinkSyncEvent;
import com.sofa.linkiving.domain.link.event.LinkSyncEventListener;
import com.sofa.linkiving.domain.link.facade.SummaryWorkerFacade;
import com.sofa.linkiving.domain.link.service.SummaryDeadLetterService;
import com.sofa.linkiving.domain.link.worker.SummaryQueue;
import com.sofa.linkiving.domain.link.worker.SummaryWorker;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.service.MemberQueryService;
import com.sofa.linkiving.global.error.exception.BusinessException;

import feign.Request;
import feign.Response;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class RagRetryPolicyTest {

	@ParameterizedTest
	@CsvSource({"400,1", "401,1", "403,1", "404,1", "422,1", "408,3", "429,3", "500,3", "504,3"})
	void summaryRetriesOnlyTransientHttpFailures(int status, int attempts) {
		SummaryClient client = mock(SummaryClient.class);
		RuntimeException failure = decode(status);
		when(client.initialSummary(any())).thenThrow(failure);
		SummaryWorker worker = summaryWorker(client);
		Link link = Link.builder().member(Member.builder().email("test@example.com").build()).build();

		assertThatThrownBy(() -> worker.callAiServerWithRetry(link)).isSameAs(failure);
		verify(client, times(attempts)).initialSummary(any());
	}

	@Test
	void emptySummaryIsNotRetried() {
		SummaryClient client = mock(SummaryClient.class);
		when(client.initialSummary(any())).thenThrow(new EmptyAiResponseException());

		assertThatThrownBy(() -> summaryWorker(client).callAiServerWithRetry(
			Link.builder().member(Member.builder().email("test@example.com").build()).build()))
			.isInstanceOf(EmptyAiResponseException.class);
		verify(client).initialSummary(any());
	}

	@ParameterizedTest
	@CsvSource({"400,1", "401,1", "403,1", "422,1", "408,3", "429,3", "502,3"})
	void syncRecoversOnceAfterClassifiedAttempts(int status, int attempts) {
		LinkSyncClient client = mock(LinkSyncClient.class);
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		LinkSyncEventListener target = new LinkSyncEventListener(client, registry, mock(MemberQueryService.class));
		ReflectionTestUtils.invokeMethod(target, "initCounters");
		LinkSyncUpdateReq request = LinkSyncUpdateReq.builder().linkId(1L).build();
		doThrow(decode(status)).when(client).syncUpdate(request);

		assertThatCode(() -> proxy(target).handleLinkSyncEvent(new LinkSyncEvent(request, SyncAction.UPDATE)))
			.doesNotThrowAnyException();
		verify(client, times(attempts)).syncUpdate(request);
		assertThat(registry.getMeters().stream()
			.flatMap(meter -> java.util.stream.StreamSupport.stream(meter.measure().spliterator(), false))
			.mapToDouble(measurement -> measurement.getValue()).sum()).isEqualTo(1.0);
	}

	@ParameterizedTest
	@CsvSource({
		"CREATE,403,1", "UPDATE,403,1", "DELETE,403,1",
		"CREATE,408,3", "UPDATE,408,3", "DELETE,408,3",
		"CREATE,429,3", "UPDATE,429,3", "DELETE,429,3",
		"CREATE,500,3", "UPDATE,500,3", "DELETE,500,3"
	})
	void syncClassifiesWrappedFailuresThroughRealAdapter(SyncAction action, int status, int attempts) {
		LinkSyncFeign feign = mock(LinkSyncFeign.class);
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		RagLinkSyncClient client = new RagLinkSyncClient(feign, registry);
		ReflectionTestUtils.invokeMethod(client, "initCounters");
		LinkSyncEventListener target = new LinkSyncEventListener(client, registry, mock(MemberQueryService.class));
		ReflectionTestUtils.invokeMethod(target, "initCounters");
		LinkSyncUpdateReq request = LinkSyncUpdateReq.builder().linkId(1L).build();
		var failure = new NoFallbackAvailableException("No fallback available",
			new CompletionException(new ExecutionException(decode(status))));
		if (action == SyncAction.DELETE) {
			doThrow(failure).when(feign).syncDelete(new LinkSyncDeleteReq(1L));
		} else {
			doThrow(failure).when(feign).syncUpdate(request);
		}

		assertThatCode(() -> proxy(target).handleLinkSyncEvent(new LinkSyncEvent(request, action)))
			.doesNotThrowAnyException();

		if (action == SyncAction.DELETE) {
			verify(feign, times(attempts)).syncDelete(new LinkSyncDeleteReq(1L));
		} else {
			verify(feign, times(attempts)).syncUpdate(request);
		}
		assertThat(registry.counter("ai.client.calls", "client", "link-sync",
			"operation", action.name().toLowerCase(java.util.Locale.ROOT), "result", "failure").count())
			.isEqualTo(attempts);
		assertThat(registry.getMeters().stream()
			.filter(meter -> !meter.getId().getName().equals("ai.client.calls"))
			.flatMap(meter -> java.util.stream.StreamSupport.stream(meter.measure().spliterator(), false))
			.mapToDouble(measurement -> measurement.getValue()).sum()).isEqualTo(1.0);
	}

	@Test
	void decoderPreservesPermanentClassificationThroughAsyncWrapperWithoutBody() {
		BusinessException failure = decode(403);
		var registry = new SimpleMeterRegistry();
		var counter = registry.counter("test.failure");
		assertThat(failure).isInstanceOf(NonRetryableExternalApiException.class);
		assertThat(((NonRetryableExternalApiException)failure).getUpstreamStatus()).isEqualTo(403);
		assertThat(failure.getMessage()).doesNotContain("private-response");
		assertThat(ExternalApiSupport.handleFailure("summary", "initial", 1L, counter,
			System.nanoTime(), new CompletionException(failure))).isSameAs(failure);
		assertThat(counter.count()).isEqualTo(1.0);
	}

	@Test
	void timeoutAndRateLimitHaveDistinctErrorCodes() {
		assertThat(decode(408).getErrorCode()).isEqualTo(ExternalApiErrorCode.EXTERNAL_API_TIMEOUT);
		assertThat(decode(429).getErrorCode()).isEqualTo(ExternalApiErrorCode.EXTERNAL_API_UNAVAILABLE);
		assertThat(decode(401).getErrorCode()).isEqualTo(ExternalApiErrorCode.EXTERNAL_API_UNAUTHORIZED);
	}

	@SuppressWarnings("unchecked")
	private SummaryWorker summaryWorker(SummaryClient client) {
		return proxy(new SummaryWorker(mock(SummaryQueue.class), new SummaryWorkerProperties(Duration.ofMillis(10)),
			mock(SummaryWorkerFacade.class), client, mock(ApplicationEventPublisher.class), mock(ObjectProvider.class),
			new SimpleMeterRegistry(), mock(SummaryDeadLetterService.class), mock(SummaryAnalyticsPublisher.class),
			mock(MemberQueryService.class)));
	}

	private BusinessException decode(int status) {
		Request request = Request.create(Request.HttpMethod.POST, "http://localhost/test", Map.of(),
			new byte[0], StandardCharsets.UTF_8, null);
		Response response = Response.builder().status(status).request(request).headers(Map.of())
			.body("private-response", StandardCharsets.UTF_8).build();
		return (BusinessException)new GlobalFeignErrorDecoder().decode("test", response);
	}

	@SuppressWarnings("unchecked")
	private <T> T proxy(T target) {
		var interceptor = new AnnotationAwareRetryOperationsInterceptor();
		interceptor.setBeanFactory(new DefaultListableBeanFactory());
		// 실제 Retry 프록시를 사용하되 테스트에서 대기하지 않는다.
		interceptor.setSleeper(delay -> { });
		ProxyFactory factory = new ProxyFactory(target);
		factory.setProxyTargetClass(true);
		factory.addAdvisor(new DefaultIntroductionAdvisor(interceptor, Retryable.class));
		return (T)factory.getProxy();
	}
}
