package com.sofa.linkiving.domain.chat.facade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.dto.response.UpsertFeedbackRes;
import com.sofa.linkiving.domain.chat.entity.Feedback;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Sentiment;
import com.sofa.linkiving.domain.chat.service.FeedbackService;
import com.sofa.linkiving.domain.chat.service.MessageService;
import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackFacade 단위 테스트")
public class FeedbackFacadeTest {
	@InjectMocks
	private FeedbackFacade feedbackFacade;

	@Mock
	private FeedbackService feedbackService;

	@Mock
	private MessageService messageService;

	@Test
	@DisplayName("피드백을 생성하거나 수정하고 피드백 ID를 반환한다")
	void upsertFeedback() {
		// given
		Member member = mock(Member.class);
		Long messageId = 1L;
		Sentiment sentiment = Sentiment.LIKE;
		String text = "유용한 답변입니다.";

		Message message = mock(Message.class);
		given(messageService.get(messageId, member)).willReturn(message);

		Feedback feedback = mock(Feedback.class);
		given(feedback.getId()).willReturn(100L);
		given(feedbackService.upsertFeedback(message, sentiment, text)).willReturn(feedback);

		// when
		UpsertFeedbackRes result = feedbackFacade.upsertFeedback(member, messageId, sentiment, text);

		// then
		assertThat(result).isNotNull();
		assertThat(result.id()).isEqualTo(100L);

		verify(messageService, times(1)).get(messageId, member);
		verify(feedbackService, times(1)).upsertFeedback(message, sentiment, text);
	}
}
