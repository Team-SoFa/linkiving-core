package com.sofa.linkiving.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.entity.Feedback;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.repository.FeedbackRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackQueryService 단위 테스트")
public class FeedbackQueryServiceTest {
	@InjectMocks
	private FeedbackQueryService feedbackQueryService;

	@Mock
	private FeedbackRepository feedbackRepository;

	@Test
	@DisplayName("메시지로 피드백을 조회한다")
	void findOptionalByMessage() {
		// given
		Message message = mock(Message.class);
		Feedback feedback = mock(Feedback.class);
		given(feedbackRepository.findByMessage(message)).willReturn(Optional.of(feedback));

		// when
		Optional<Feedback> result = feedbackQueryService.findOptionalByMessage(message);

		// then
		assertThat(result).isPresent().contains(feedback);
		verify(feedbackRepository, times(1)).findByMessage(message);
	}

	@Test
	@DisplayName("메시지에 해당하는 피드백이 없으면 빈 Optional을 반환한다")
	void findOptionalByMessage_ReturnsEmpty() {
		// given
		Message message = mock(Message.class);
		given(feedbackRepository.findByMessage(message)).willReturn(Optional.empty());

		// when
		Optional<Feedback> result = feedbackQueryService.findOptionalByMessage(message);

		// then
		assertThat(result).isEmpty();
		verify(feedbackRepository, times(1)).findByMessage(message);
	}
}
