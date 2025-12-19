package com.sofa.linkiving.domain.chat.facade;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.dto.response.AddFeedbackRes;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Sentiment;
import com.sofa.linkiving.domain.chat.service.FeedbackService;
import com.sofa.linkiving.domain.chat.service.MessageService;
import com.sofa.linkiving.domain.member.entity.Member;

@ExtendWith(MockitoExtension.class)
public class FeedbackFacadeTest {
	@InjectMocks
	private FeedbackFacade feedbackFacade;

	@Mock
	private FeedbackService feedbackService;

	@Mock
	private MessageService messageService;

	@Test
	@DisplayName("피드백 생성 요청 시 Message 조회 후 Feedback을 생성하고 결과를 반환함")
	void shouldCreateFeedbackAndReturnRes() {
		// given
		Long messageId = 1L;
		Long feedbackId = 100L;
		Sentiment sentiment = Sentiment.LIKE;
		String text = "도움이 되었어요";

		Message message = mock(Message.class);
		Member member = mock(Member.class);

		given(messageService.get(messageId, member)).willReturn(message);
		given(feedbackService.create(message, sentiment, text)).willReturn(feedbackId);

		// when
		AddFeedbackRes result = feedbackFacade.createFeedback(member, messageId, sentiment, text);

		// then
		assertThat(result).isNotNull();
		assertThat(result.id()).isEqualTo(feedbackId);

		verify(messageService).get(messageId, member);
		verify(feedbackService).create(message, sentiment, text);
	}
}
