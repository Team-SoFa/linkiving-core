package com.sofa.linkiving.domain.link.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.sofa.linkiving.domain.link.entity.SummaryDeadLetter;
import com.sofa.linkiving.domain.link.enums.DeadLetterStatus;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;
import com.sofa.linkiving.domain.link.error.SummaryErrorCode;
import com.sofa.linkiving.domain.link.repository.SummaryDeadLetterRepository;
import com.sofa.linkiving.domain.link.worker.SummaryQueue;
import com.sofa.linkiving.global.error.exception.BusinessException;
import com.sofa.linkiving.infra.feign.ExternalApiErrorCode;

@ExtendWith(MockitoExtension.class)
class SummaryDeadLetterServiceTest {

	@Mock
	private SummaryDeadLetterRepository deadLetterRepository;

	@Mock
	private SummaryQueue summaryQueue;

	@Mock
	private LinkService linkService;

	@InjectMocks
	private SummaryDeadLetterService summaryDeadLetterService;

	@Test
	@DisplayName("record: BusinessException 원인이면 errorCode 를 추출해 PENDING 으로 적재한다")
	void record_withBusinessException() {
		BusinessException cause = new BusinessException(ExternalApiErrorCode.EXTERNAL_API_COMMUNICATION_ERROR);

		summaryDeadLetterService.record(10L, 20L, cause);

		ArgumentCaptor<SummaryDeadLetter> captor = ArgumentCaptor.forClass(SummaryDeadLetter.class);
		verify(deadLetterRepository).save(captor.capture());
		SummaryDeadLetter saved = captor.getValue();
		assertThat(saved.getLinkId()).isEqualTo(10L);
		assertThat(saved.getMemberId()).isEqualTo(20L);
		assertThat(saved.getErrorCode()).isEqualTo("E_000");
		assertThat(saved.getExceptionType()).isEqualTo("BusinessException");
		assertThat(saved.getStatus()).isEqualTo(DeadLetterStatus.PENDING);
	}

	@Test
	@DisplayName("record: 일반 예외면 errorCode 는 null, 예외 타입/메시지를 기록한다")
	void record_withGenericException() {
		Throwable cause = new RuntimeException("boom");

		summaryDeadLetterService.record(10L, null, cause);

		ArgumentCaptor<SummaryDeadLetter> captor = ArgumentCaptor.forClass(SummaryDeadLetter.class);
		verify(deadLetterRepository).save(captor.capture());
		SummaryDeadLetter saved = captor.getValue();
		assertThat(saved.getErrorCode()).isNull();
		assertThat(saved.getExceptionType()).isEqualTo("RuntimeException");
		assertThat(saved.getFailureReason()).isEqualTo("boom");
		assertThat(saved.getMemberId()).isNull();
	}

	@Test
	@DisplayName("record: cause 메시지가 null 이면 failureReason 도 null 이다")
	void record_withNullMessageCause() {
		summaryDeadLetterService.record(1L, 2L, new RuntimeException());

		ArgumentCaptor<SummaryDeadLetter> captor = ArgumentCaptor.forClass(SummaryDeadLetter.class);
		verify(deadLetterRepository).save(captor.capture());
		assertThat(captor.getValue().getFailureReason()).isNull();
	}

	@Test
	@DisplayName("record: 1000자를 넘는 메시지는 1000자로 잘려 저장된다")
	void record_withLongMessageCause() {
		String longMessage = "x".repeat(1500);

		summaryDeadLetterService.record(1L, 2L, new RuntimeException(longMessage));

		ArgumentCaptor<SummaryDeadLetter> captor = ArgumentCaptor.forClass(SummaryDeadLetter.class);
		verify(deadLetterRepository).save(captor.capture());
		assertThat(captor.getValue().getFailureReason()).hasSize(1000);
	}

	@Test
	@DisplayName("getDeadLetters: status 가 null 이면 findAll 을 호출한다")
	void getDeadLetters_nullStatus() {
		Pageable pageable = PageRequest.of(0, 20);
		given(deadLetterRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of()));

		Page<SummaryDeadLetter> result = summaryDeadLetterService.getDeadLetters(null, pageable);

		assertThat(result).isNotNull();
		verify(deadLetterRepository).findAll(pageable);
		verify(deadLetterRepository, never()).findAllByStatus(any(), any());
	}

	@Test
	@DisplayName("getDeadLetters: status 가 있으면 findAllByStatus 를 호출한다")
	void getDeadLetters_withStatus() {
		Pageable pageable = PageRequest.of(0, 20);
		given(deadLetterRepository.findAllByStatus(DeadLetterStatus.PENDING, pageable))
			.willReturn(new PageImpl<>(List.of()));

		summaryDeadLetterService.getDeadLetters(DeadLetterStatus.PENDING, pageable);

		verify(deadLetterRepository).findAllByStatus(DeadLetterStatus.PENDING, pageable);
		verify(deadLetterRepository, never()).findAll(any(Pageable.class));
	}

	@Test
	@DisplayName("reprocess: PENDING 이면 링크 상태 PENDING 복귀 + 큐 재적재 + REPROCESSED 로 변경")
	void reprocess_pending() {
		SummaryDeadLetter deadLetter = SummaryDeadLetter.builder().linkId(10L).build();
		given(deadLetterRepository.findById(1L)).willReturn(Optional.of(deadLetter));

		summaryDeadLetterService.reprocess(1L);

		verify(linkService).updateSummaryStatus(10L, SummaryStatus.PENDING);
		verify(summaryQueue).addToQueue(10L);
		assertThat(deadLetter.getStatus()).isEqualTo(DeadLetterStatus.REPROCESSED);
		assertThat(deadLetter.getReprocessedAt()).isNotNull();
	}

	@Test
	@DisplayName("reprocess: 트랜잭션이 활성일 때는 커밋 이후에 큐에 재적재한다")
	void reprocess_enqueuesAfterCommit() {
		SummaryDeadLetter deadLetter = SummaryDeadLetter.builder().linkId(10L).build();
		given(deadLetterRepository.findById(1L)).willReturn(Optional.of(deadLetter));

		TransactionSynchronizationManager.initSynchronization();
		try {
			summaryDeadLetterService.reprocess(1L);
			// 커밋 전이므로 아직 재적재되지 않아야 한다
			verify(summaryQueue, never()).addToQueue(anyLong());
			// 커밋 콜백을 수동으로 실행
			for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
				sync.afterCommit();
			}
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}

		verify(summaryQueue).addToQueue(10L);
		assertThat(deadLetter.getStatus()).isEqualTo(DeadLetterStatus.REPROCESSED);
	}

	@Test
	@DisplayName("reprocess: 대상이 없으면 DEAD_LETTER_NOT_FOUND")
	void reprocess_notFound() {
		given(deadLetterRepository.findById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> summaryDeadLetterService.reprocess(1L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode").isEqualTo(SummaryErrorCode.DEAD_LETTER_NOT_FOUND);
		verify(linkService, never()).updateSummaryStatus(anyLong(), any());
		verify(summaryQueue, never()).addToQueue(anyLong());
	}

	@Test
	@DisplayName("reprocess: 이미 REPROCESSED 면 DEAD_LETTER_NOT_REPROCESSABLE, 부수효과 없음")
	void reprocess_alreadyReprocessed() {
		SummaryDeadLetter deadLetter = SummaryDeadLetter.builder().linkId(10L).build();
		deadLetter.markReprocessed();
		given(deadLetterRepository.findById(1L)).willReturn(Optional.of(deadLetter));

		assertThatThrownBy(() -> summaryDeadLetterService.reprocess(1L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode").isEqualTo(SummaryErrorCode.DEAD_LETTER_NOT_REPROCESSABLE);
		verify(linkService, never()).updateSummaryStatus(anyLong(), any());
		verify(summaryQueue, never()).addToQueue(anyLong());
	}

	@Test
	@DisplayName("ignore: IGNORED 로 변경한다")
	void ignore_success() {
		SummaryDeadLetter deadLetter = SummaryDeadLetter.builder().linkId(10L).build();
		given(deadLetterRepository.findById(1L)).willReturn(Optional.of(deadLetter));

		summaryDeadLetterService.ignore(1L);

		assertThat(deadLetter.getStatus()).isEqualTo(DeadLetterStatus.IGNORED);
	}

	@Test
	@DisplayName("ignore: 대상이 없으면 DEAD_LETTER_NOT_FOUND")
	void ignore_notFound() {
		given(deadLetterRepository.findById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> summaryDeadLetterService.ignore(1L))
			.isInstanceOf(BusinessException.class)
			.extracting("errorCode").isEqualTo(SummaryErrorCode.DEAD_LETTER_NOT_FOUND);
	}
}
