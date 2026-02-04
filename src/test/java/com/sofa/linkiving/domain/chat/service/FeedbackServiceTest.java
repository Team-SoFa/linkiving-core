package com.sofa.linkiving.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Feedback;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Sentiment;

@ExtendWith(MockitoExtension.class)
public class FeedbackServiceTest {

	@InjectMocks
	private FeedbackService feedbackService;

	@Mock
	private FeedbackCommandService feedbackCommandService;

	@Mock
	private FeedbackQueryService feedbackQueryService;

	@Test
	@DisplayName("피드백 생성 요청 시 엔티티를 생성하고 저장을 요청함")
	void shouldCreateAndSaveFeedback() {
		// given
		Message message = mock(Message.class);
		Sentiment sentiment = Sentiment.LIKE;
		String text = "답변이 훌륭합니다.";

		Feedback savedFeedback = mock(Feedback.class);
		given(savedFeedback.getId()).willReturn(10L);

		given(feedbackCommandService.save(any(Feedback.class))).willAnswer(invocation -> {
			Feedback argument = invocation.getArgument(0);
			assertThat(argument.getMessage()).isEqualTo(message);
			assertThat(argument.getSentiment()).isEqualTo(sentiment);
			assertThat(argument.getText()).isEqualTo(text);

			return savedFeedback;
		});

		// when
		Long resultId = feedbackService.create(message, sentiment, text);

		// then
		assertThat(resultId).isEqualTo(10L);
		verify(feedbackCommandService).save(any(Feedback.class));
	}

	@Test
	@DisplayName("FeedbackCommandService.deleteAllByMessageIn 호출 위임")
	void shouldCallDeleteAllByMessageInWhenDeleteFeedbacks() {
		// given
		Chat chat = mock(Chat.class);

		// when
		feedbackService.deleteAll(chat);

		// then
		verify(feedbackCommandService).deleteFeedbacksByChat(chat);
	}
}
