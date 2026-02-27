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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.link.ai.SummaryClient;
import com.sofa.linkiving.domain.link.config.SummaryWorkerProperties;
import com.sofa.linkiving.domain.link.dto.response.RagInitialSummaryRes;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.service.LinkService;
import com.sofa.linkiving.domain.link.service.SummaryService;
import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
@DisplayName("SummaryWorker 단위 테스트")
class SummaryWorkerTest {

	@Mock
	private SummaryQueue summaryQueue;
	@Mock
	private SummaryService summaryService;
	@Mock
	private LinkService linkService;
	@Mock
	private SummaryClient summaryClient;

	private SummaryWorker summaryWorker;

	@BeforeEach
	void setUp() {
		SummaryWorkerProperties properties = new SummaryWorkerProperties(Duration.ofMillis(10));
		summaryWorker = new SummaryWorker(summaryQueue, properties, summaryService, linkService, summaryClient);
	}

	@AfterEach
	void tearDown() {
		summaryWorker.stopWorker();
	}

	@Test
	@DisplayName("큐에 링크가 있으면 정상적으로 AI 요약을 요청하고 저장")
	void shouldProcessLinkAndSaveSummary() {
		// given
		Long linkId = 1L;
		given(summaryQueue.pollFromQueue())
			.willReturn(Optional.of(linkId))
			.willReturn(Optional.empty());

		Link link = mock(Link.class);
		Member member = mock(Member.class);

		// Link 엔티티 Mocking
		given(link.getId()).willReturn(linkId);
		given(link.getMember()).willReturn(member);
		given(member.getId()).willReturn(100L);
		given(link.getUrl()).willReturn("http://test.com");
		given(link.getTitle()).willReturn("Test Title");
		given(link.getMemo()).willReturn("Test Memo");

		given(linkService.getLink(linkId)).willReturn(link);

		// AI 클라이언트 응답 Mocking
		RagInitialSummaryRes mockRes = mock(RagInitialSummaryRes.class);
		given(mockRes.summary()).willReturn("요약된 내용입니다.");
		given(summaryClient.initialSummary(linkId, 100L, "Test Title", "http://test.com", "Test Memo"))
			.willReturn(mockRes);

		// when
		summaryWorker.startWorker();

		// then
		verify(summaryService, timeout(1000).times(1)).createInitialSummary(link, "요약된 내용입니다.");
	}

	@Test
	@DisplayName("AI 응답이 null일 경우 요약을 생성하지 않음")
	void shouldNotSaveSummary_WhenClientReturnsNull() {
		// given
		Long linkId = 2L;
		given(summaryQueue.pollFromQueue())
			.willReturn(Optional.of(linkId))
			.willReturn(Optional.empty());

		Link link = mock(Link.class);
		Member member = mock(Member.class);
		given(link.getId()).willReturn(linkId);
		given(link.getMember()).willReturn(member);
		given(member.getId()).willReturn(100L);

		given(linkService.getLink(linkId)).willReturn(link);

		// AI 응답이 null로 반환되는 상황
		given(summaryClient.initialSummary(anyLong(), anyLong(), any(), any(), any())).willReturn(null);

		// when
		summaryWorker.startWorker();

		// then
		// 클라이언트 호출은 일어났으나
		verify(summaryClient, timeout(1000).times(1)).initialSummary(anyLong(), anyLong(), any(), any(), any());
		// 저장은 호출되지 않아야 함
		verify(summaryService, after(200).never()).createInitialSummary(any(Link.class), anyString());
	}

	@Test
	@DisplayName("처리 중 예외가 발생해도 워커 쓰레드는 종료되지 않고 다음 큐를 계속 확인")
	void shouldContinueWorking_WhenExceptionOccurs() {
		// given
		Long linkId = 3L;
		given(summaryQueue.pollFromQueue())
			.willReturn(Optional.of(linkId))
			.willReturn(Optional.empty());

		// Link 조회 중 강제로 RuntimeException 발생
		given(linkService.getLink(linkId)).willThrow(new RuntimeException("DB Connection Error"));

		// when
		summaryWorker.startWorker();

		// then
		verify(linkService, timeout(1000).times(1)).getLink(linkId);

		// 예외를 catch 블록에서 먹고 루프가 계속 도는지 검증 (최소 2번 이상 poll 호출 여부)
		verify(summaryQueue, timeout(1000).atLeast(2)).pollFromQueue();
		verify(summaryService, never()).createInitialSummary(any(), any());
	}

	@Test
	@DisplayName("큐가 비어있으면 지정된 시간만큼 Sleep 후 다시 확인")
	void shouldSleepAndRetry_WhenQueueIsEmpty() {
		// given
		given(summaryQueue.pollFromQueue()).willReturn(Optional.empty());

		// when
		summaryWorker.startWorker();

		// then
		// 10ms 단위로 대기하므로, 짧은 시간 내에 여러 번 pollFromQueue를 호출하는지 확인
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

		// Link 1 Mocking
		Link link1 = mock(Link.class);
		Member member1 = mock(Member.class);
		lenient().when(link1.getId()).thenReturn(linkId1);
		lenient().when(link1.getMember()).thenReturn(member1);
		lenient().when(member1.getId()).thenReturn(100L);

		// Link 2 Mocking
		Link link2 = mock(Link.class);
		Member member2 = mock(Member.class);
		lenient().when(link2.getId()).thenReturn(linkId2);
		lenient().when(link2.getMember()).thenReturn(member2);
		lenient().when(member2.getId()).thenReturn(200L);

		given(linkService.getLink(linkId1)).willReturn(link1);
		given(linkService.getLink(linkId2)).willReturn(link2);

		// Client 응답 Mocking
		RagInitialSummaryRes mockRes1 = mock(RagInitialSummaryRes.class);
		given(mockRes1.summary()).willReturn("Summary 1");
		given(summaryClient.initialSummary(eq(linkId1), anyLong(), any(), any(), any())).willReturn(mockRes1);

		RagInitialSummaryRes mockRes2 = mock(RagInitialSummaryRes.class);
		given(mockRes2.summary()).willReturn("Summary 2");
		given(summaryClient.initialSummary(eq(linkId2), anyLong(), any(), any(), any())).willReturn(mockRes2);

		// when
		summaryWorker.startWorker();

		// then
		InOrder inOrder = inOrder(summaryService);
		inOrder.verify(summaryService, timeout(1000).times(1)).createInitialSummary(link1, "Summary 1");
		inOrder.verify(summaryService, timeout(1000).times(1)).createInitialSummary(link2, "Summary 2");
	}

	@Test
	@DisplayName("워커 실행 시 메인 쓰레드가 차단되지 않고 백그라운드 쓰레드(summary-worker)에서 동작함")
	void shouldRunInBackgroundThread() {
		// given
		String mainThreadName = Thread.currentThread().getName();
		String[] workerThreadName = new String[1];

		given(summaryQueue.pollFromQueue()).willAnswer(invocation -> {
			workerThreadName[0] = Thread.currentThread().getName();
			return Optional.empty(); // 무한 루프 방지
		});

		// when
		summaryWorker.startWorker();

		// then
		// 백그라운드 쓰레드가 큐를 확인하는 로직이 호출될 때까지 대기
		verify(summaryQueue, timeout(1000).atLeastOnce()).pollFromQueue();

		assertThat(workerThreadName[0]).isNotNull();
		assertThat(workerThreadName[0]).isNotEqualTo(mainThreadName);
		assertThat(workerThreadName[0]).isEqualTo("summary-worker");
	}
}
