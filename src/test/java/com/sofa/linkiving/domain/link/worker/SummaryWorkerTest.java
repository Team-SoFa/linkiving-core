package com.sofa.linkiving.domain.link.worker;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import com.sofa.linkiving.domain.link.ai.SummaryClient;
import com.sofa.linkiving.domain.link.config.SummaryWorkerProperties;
import com.sofa.linkiving.domain.link.dto.response.RagInitialSummaryRes;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;
import com.sofa.linkiving.domain.link.event.SummaryStatusEvent;
import com.sofa.linkiving.domain.link.facade.SummaryWorkerFacade;
import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
@DisplayName("SummaryWorker 단위 테스트")
class SummaryWorkerTest {

	@Mock
	private SummaryQueue summaryQueue;
	@Mock
	private SummaryWorkerFacade summaryWorkerFacade;
	@Mock
	private SummaryClient summaryClient;
	@Mock
	private ApplicationEventPublisher eventPublisher;
	@Mock
	private ObjectProvider<SummaryWorker> selfProvider;

	private SummaryWorker summaryWorker;
	private Link mockLink;
	private Member mockMember;

	@BeforeEach
	void setUp() {
		SummaryWorkerProperties properties = new SummaryWorkerProperties(Duration.ofMillis(10));
		summaryWorker = new SummaryWorker(summaryQueue, properties, summaryWorkerFacade, summaryClient, eventPublisher,
			selfProvider);

		mockLink = mock(Link.class);
		mockMember = mock(Member.class);

		lenient().when(mockLink.getId()).thenReturn(1L);
		lenient().when(mockLink.getMember()).thenReturn(mockMember);
		lenient().when(mockLink.getSummaryStatus()).thenReturn(SummaryStatus.PENDING);
		lenient().when(mockMember.getId()).thenReturn(100L);
		lenient().when(mockMember.getEmail()).thenReturn("test@test.com");

		lenient().when(selfProvider.getObject()).thenReturn(summaryWorker);
	}

	@AfterEach
	void tearDown() {
		summaryWorker.stopWorker();
	}

	@Test
	@DisplayName("워커 실행 시 메인 쓰레드가 차단되지 않고 백그라운드 쓰레드(summary-worker)에서 동작함")
	void shouldRunInBackgroundThread() {
		// given
		String mainThreadName = Thread.currentThread().getName();
		String[] workerThreadName = new String[1];

		given(summaryQueue.pollFromQueue()).willAnswer(invocation -> {
			workerThreadName[0] = Thread.currentThread().getName();
			return Optional.empty();
		});

		// when
		summaryWorker.startWorker();

		// then
		verify(summaryQueue, timeout(1000).atLeastOnce()).pollFromQueue();

		assertThat(workerThreadName[0]).isNotNull();
		assertThat(workerThreadName[0]).isNotEqualTo(mainThreadName);
		assertThat(workerThreadName[0]).isEqualTo("summary-worker");
	}

	@Test
	@DisplayName("워커 루프 내부에서 예상치 못한 예외 발생 시 에러를 로깅하고 루프를 계속 실행함")
	void shouldCatchExceptionAndContinueLoop_WhenUnexpectedErrorOccurs() {
		// given
		given(summaryQueue.pollFromQueue())
			.willThrow(new RuntimeException("Unexpected Error"))
			.willReturn(Optional.empty());

		// when
		summaryWorker.startWorker();

		// then
		verify(summaryQueue, timeout(1000).atLeast(2)).pollFromQueue();
	}

	@Test
	@DisplayName("PENDING 상태가 아닌 링크는 AI 요약을 건너뛴다")
	void shouldSkipIfNotPending() {
		// given
		given(summaryQueue.pollFromQueue())
			.willReturn(Optional.of(1L))
			.willReturn(Optional.empty());

		given(summaryWorkerFacade.getLinkWithMember(1L)).willReturn(mockLink);
		given(mockLink.getSummaryStatus()).willReturn(SummaryStatus.PROCESSING);

		// when
		summaryWorker.startWorker();

		// then
		verify(eventPublisher, after(200).never()).publishEvent(any());
		verify(summaryWorkerFacade, never()).updateSummaryStatus(anyLong(), any());
	}

	@Test
	@DisplayName("큐에 링크가 있으면 정상적으로 AI 요약을 요청하고 저장")
	void shouldProcessLinkAndSaveSummary() {
		// given
		Long linkId = 1L;
		given(summaryQueue.pollFromQueue())
			.willReturn(Optional.of(linkId))
			.willReturn(Optional.empty());

		given(summaryWorkerFacade.getLinkWithMember(linkId)).willReturn(mockLink);
		given(mockLink.getUrl()).willReturn("http://test.com");
		given(mockLink.getTitle()).willReturn("Test Title");
		given(mockLink.getMemo()).willReturn("Test Memo");

		RagInitialSummaryRes mockRes = mock(RagInitialSummaryRes.class);
		given(mockRes.summary()).willReturn("요약된 내용입니다.");
		given(summaryClient.initialSummary(linkId, 100L, "Test Title", "http://test.com", "Test Memo"))
			.willReturn(mockRes);

		// when
		summaryWorker.startWorker();

		// then
		verify(summaryWorkerFacade, timeout(1000).times(1)).createInitialSummaryAndUpdateStatus(linkId, "요약된 내용입니다.");
	}

	@Test
	@DisplayName("처리 중 예외가 발생해도 워커 쓰레드는 종료되지 않고 다음 큐를 계속 확인")
	void shouldContinueWorking_WhenExceptionOccurs() {
		// given
		Long linkId = 3L;
		given(summaryQueue.pollFromQueue())
			.willReturn(Optional.of(linkId))
			.willReturn(Optional.empty());

		given(summaryWorkerFacade.getLinkWithMember(linkId)).willThrow(new RuntimeException("DB Connection Error"));

		// when
		summaryWorker.startWorker();

		// then
		verify(summaryWorkerFacade, timeout(1000).times(2)).getLinkWithMember(linkId);
		verify(summaryQueue, timeout(1000).atLeast(2)).pollFromQueue();
		verify(summaryWorkerFacade, never()).createInitialSummaryAndUpdateStatus(any(), any());
	}

	@Test
	@DisplayName("큐가 비어있으면 지정된 시간만큼 Sleep 후 다시 확인")
	void shouldSleepAndRetry_WhenQueueIsEmpty() {
		// given
		given(summaryQueue.pollFromQueue()).willReturn(Optional.empty());

		// when
		summaryWorker.startWorker();

		// then
		verify(summaryQueue, timeout(500).atLeast(3)).pollFromQueue();
	}

	@Test
	@DisplayName("여러 링크가 큐에 있을 때 들어온 순서대로 처리함")
	void shouldProcessQueueSequentially() {
		// given
		Long linkId1 = 10L;
		Long linkId2 = 20L;

		given(summaryQueue.pollFromQueue())
			.willReturn(Optional.of(linkId1))
			.willReturn(Optional.of(linkId2))
			.willReturn(Optional.empty());

		Link link1 = mock(Link.class);
		Member member1 = mock(Member.class);
		lenient().when(link1.getId()).thenReturn(linkId1);
		lenient().when(link1.getSummaryStatus()).thenReturn(SummaryStatus.PENDING);
		lenient().when(link1.getMember()).thenReturn(member1);
		lenient().when(member1.getId()).thenReturn(100L);

		Link link2 = mock(Link.class);
		Member member2 = mock(Member.class);
		lenient().when(link2.getId()).thenReturn(linkId2);
		lenient().when(link2.getSummaryStatus()).thenReturn(SummaryStatus.PENDING);
		lenient().when(link2.getMember()).thenReturn(member2);
		lenient().when(member2.getId()).thenReturn(200L);

		given(summaryWorkerFacade.getLinkWithMember(linkId1)).willReturn(link1);
		given(summaryWorkerFacade.getLinkWithMember(linkId2)).willReturn(link2);

		RagInitialSummaryRes mockRes1 = mock(RagInitialSummaryRes.class);
		given(mockRes1.summary()).willReturn("Summary 1");
		given(summaryClient.initialSummary(eq(linkId1), anyLong(), any(), any(), any())).willReturn(mockRes1);

		RagInitialSummaryRes mockRes2 = mock(RagInitialSummaryRes.class);
		given(mockRes2.summary()).willReturn("Summary 2");
		given(summaryClient.initialSummary(eq(linkId2), anyLong(), any(), any(), any())).willReturn(mockRes2);

		// when
		summaryWorker.startWorker();

		// then
		InOrder inOrder = inOrder(summaryWorkerFacade);
		inOrder.verify(summaryWorkerFacade, timeout(1000).times(1))
			.createInitialSummaryAndUpdateStatus(linkId1, "Summary 1");
		inOrder.verify(summaryWorkerFacade, timeout(1000).times(1))
			.createInitialSummaryAndUpdateStatus(linkId2, "Summary 2");
	}

	@Test
	@DisplayName("정상 처리 시 PROCESSING 및 COMPLETED 이벤트를 순차적으로 발행함")
	void shouldPublishProcessingAndCompletedEvents_WhenSuccess() {
		// given
		given(summaryQueue.pollFromQueue())
			.willReturn(Optional.of(1L))
			.willReturn(Optional.empty());

		given(summaryWorkerFacade.getLinkWithMember(1L)).willReturn(mockLink);

		RagInitialSummaryRes mockRes = mock(RagInitialSummaryRes.class);
		given(mockRes.summary()).willReturn("요약 완료");
		given(summaryClient.initialSummary(anyLong(), anyLong(), any(), any(), any())).willReturn(mockRes);

		Summary mockSummary = mock(Summary.class);
		given(mockSummary.getId()).willReturn(10L);
		given(mockSummary.getContent()).willReturn("요약 완료");
		given(summaryWorkerFacade.createInitialSummaryAndUpdateStatus(eq(1L), anyString())).willReturn(mockSummary);

		// when
		summaryWorker.startWorker();

		// then
		ArgumentCaptor<SummaryStatusEvent> captor = ArgumentCaptor.forClass(SummaryStatusEvent.class);
		verify(eventPublisher, timeout(1000).times(2)).publishEvent(captor.capture());

		assertThat(captor.getAllValues().get(0).response().status()).isEqualTo(SummaryStatus.PROCESSING);
		assertThat(captor.getAllValues().get(1).response().status()).isEqualTo(SummaryStatus.COMPLETED);
	}

	@Test
	@DisplayName("AI 응답이 null일 경우 예외가 발생하여 FAILED 이벤트를 발행함")
	void shouldPublishFailedEvent_WhenAiResponseIsNull() {
		// given
		given(summaryQueue.pollFromQueue())
			.willReturn(Optional.of(1L))
			.willReturn(Optional.empty());

		given(summaryWorkerFacade.getLinkWithMember(1L)).willReturn(mockLink);

		// AI 응답이 null이면 callAiServerWithRetry 내에서 RuntimeException 발생
		given(summaryClient.initialSummary(anyLong(), anyLong(), any(), any(), any())).willReturn(null);

		// when
		summaryWorker.startWorker();

		// then
		ArgumentCaptor<SummaryStatusEvent> captor = ArgumentCaptor.forClass(SummaryStatusEvent.class);
		verify(eventPublisher, timeout(1000).times(2)).publishEvent(captor.capture());

		assertThat(captor.getAllValues().get(0).response().status()).isEqualTo(SummaryStatus.PROCESSING);
		assertThat(captor.getAllValues().get(1).response().status()).isEqualTo(SummaryStatus.FAILED);
		assertThat(captor.getAllValues().get(1).response().data()).isInstanceOf(String.class)
			.extracting(data -> (String)data)
			.asString()
			.contains("Retry limit exceeded");
	}

	@Test
	@DisplayName("처리 중 내부 예외 발생 시 FAILED 이벤트를 발행함")
	void shouldPublishFailedEvent_WhenExceptionOccurs() {
		// given
		given(summaryQueue.pollFromQueue())
			.willReturn(Optional.of(1L))
			.willReturn(Optional.empty());

		given(summaryWorkerFacade.getLinkWithMember(1L)).willReturn(mockLink);

		// AI 요청 단계에서 강제 예외 발생 유도
		given(summaryClient.initialSummary(anyLong(), anyLong(), any(), any(), any()))
			.willThrow(new RuntimeException("Network Error"));

		// when
		summaryWorker.startWorker();

		// then
		ArgumentCaptor<SummaryStatusEvent> captor = ArgumentCaptor.forClass(SummaryStatusEvent.class);
		verify(eventPublisher, timeout(1000).times(2)).publishEvent(captor.capture());

		assertThat(captor.getAllValues().get(0).response().status()).isEqualTo(SummaryStatus.PROCESSING);
		assertThat(captor.getAllValues().get(1).response().status()).isEqualTo(SummaryStatus.FAILED);
		assertThat(captor.getAllValues().get(1).response().data()).isInstanceOf(String.class)
			.extracting(data -> (String)data)
			.asString()
			.contains("Retry limit exceeded");
	}

	@Test
	@DisplayName("Facade에서 요약 생성 결과가 null일 경우 COMPLETED 이벤트를 발행하지 않는다")
	void shouldNotPublishCompletedEvent_WhenFacadeReturnsNull() {
		// given
		given(summaryQueue.pollFromQueue())
			.willReturn(Optional.of(1L))
			.willReturn(Optional.empty());

		given(summaryWorkerFacade.getLinkWithMember(1L)).willReturn(mockLink);

		RagInitialSummaryRes mockRes = mock(RagInitialSummaryRes.class);
		given(mockRes.summary()).willReturn("요약된 내용");
		given(summaryClient.initialSummary(anyLong(), anyLong(), any(), any(), any())).willReturn(mockRes);

		given(summaryWorkerFacade.createInitialSummaryAndUpdateStatus(anyLong(), anyString())).willReturn(null);

		// when
		summaryWorker.startWorker();

		// then
		ArgumentCaptor<SummaryStatusEvent> captor = ArgumentCaptor.forClass(SummaryStatusEvent.class);
		verify(eventPublisher, timeout(1000).times(1)).publishEvent(captor.capture());

		assertThat(captor.getValue().response().status()).isEqualTo(SummaryStatus.PROCESSING);
	}

	@Test
	@DisplayName("userEmail 추출 전(조회 단계) 예외가 발생하면 웹소켓 이벤트 발행 없이 루프를 계속한다")
	void shouldNotPublishEvent_WhenExceptionOccursBeforeEmailExtraction() {
		// given
		given(summaryQueue.pollFromQueue())
			.willReturn(Optional.of(1L))
			.willReturn(Optional.empty());

		given(summaryWorkerFacade.getLinkWithMember(1L)).willThrow(new RuntimeException("DB Connection Error"));

		// when
		summaryWorker.startWorker();

		// then
		verify(eventPublisher, after(500).never()).publishEvent(any());
		verify(summaryQueue, timeout(1000).atLeast(2)).pollFromQueue();
	}

	@Test
	@DisplayName("예외 복구(catch) 중 DB 상태 업데이트(inner catch)에 실패해도 FAILED 이벤트는 정상 발행된다")
	void shouldPublishFailedEvent_EvenIfInnerStatusUpdateFails() {
		// given
		given(summaryQueue.pollFromQueue())
			.willReturn(Optional.of(1L))
			.willReturn(Optional.empty());

		given(summaryWorkerFacade.getLinkWithMember(1L)).willReturn(mockLink);

		given(summaryClient.initialSummary(anyLong(), anyLong(), any(), any(), any()))
			.willThrow(new RuntimeException("Network Error"));

		willDoNothing().given(summaryWorkerFacade)
			.updateSummaryStatus(anyLong(), eq(SummaryStatus.PROCESSING));

		willThrow(new RuntimeException("DB Update Error"))
			.given(summaryWorkerFacade).updateSummaryStatus(anyLong(), eq(SummaryStatus.FAILED));

		// when
		summaryWorker.startWorker();

		// then
		ArgumentCaptor<SummaryStatusEvent> captor = ArgumentCaptor.forClass(SummaryStatusEvent.class);
		verify(eventPublisher, timeout(1000).times(2)).publishEvent(captor.capture());

		assertThat(captor.getAllValues().get(0).response().status()).isEqualTo(SummaryStatus.PROCESSING);
		assertThat(captor.getAllValues().get(1).response().status()).isEqualTo(SummaryStatus.FAILED);
		assertThat(captor.getAllValues().get(1).response().data()).isInstanceOf(String.class)
			.extracting(data -> (String)data)
			.asString()
			.contains("Retry limit exceeded");
	}
}
