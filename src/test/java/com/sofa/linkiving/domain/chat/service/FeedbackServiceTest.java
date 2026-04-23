package com.sofa.linkiving.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Feedback;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Sentiment;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackService 단위 테스트")
public class FeedbackServiceTest {

	@InjectMocks
	private FeedbackService feedbackService;

	@Mock
	private FeedbackCommandService feedbackCommandService;

	@Mock
	private FeedbackQueryService feedbackQueryService;

	@Test
	@DisplayName("기존 피드백이 존재하면 내용을 업데이트하고 반환한다")
	void shouldUpdateExistingFeedback() {
		// given
		Message message = mock(Message.class);
		Sentiment sentiment = Sentiment.LIKE;
		String text = "수정된 피드백";

		Feedback existingFeedback = mock(Feedback.class);
		given(feedbackQueryService.findOptionalByMessage(message)).willReturn(Optional.of(existingFeedback));

		// when
		Feedback result = feedbackService.upsertFeedback(message, sentiment, text);

		// then
		assertThat(result).isEqualTo(existingFeedback);
		verify(existingFeedback, times(1)).update(text, sentiment);
		verify(feedbackCommandService, never()).save(any());
	}

	@Test
	@DisplayName("기존 피드백이 없으면 새로 생성하여 저장을 요청한다")
	void shouldCreateAndSaveNewFeedback() {
		// given
		Message message = mock(Message.class);
		Sentiment sentiment = Sentiment.DISLIKE;
		String text = "새로운 피드백";

		given(feedbackQueryService.findOptionalByMessage(message)).willReturn(Optional.empty());

		Feedback savedFeedback = mock(Feedback.class);
		given(feedbackCommandService.save(any(Feedback.class))).willReturn(savedFeedback);

		// when
		Feedback result = feedbackService.upsertFeedback(message, sentiment, text);

		// then
		assertThat(result).isEqualTo(savedFeedback);

		ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
		verify(feedbackCommandService, times(1)).save(captor.capture());

		Feedback capturedFeedback = captor.getValue();
		assertThat(capturedFeedback.getMessage()).isEqualTo(message);
		assertThat(capturedFeedback.getSentiment()).isEqualTo(sentiment);
		assertThat(capturedFeedback.getText()).isEqualTo(text);
	}

	@Test
	@DisplayName("채팅에 속한 모든 피드백 삭제를 CommandService에 위임한다")
	void shouldCallDeleteAllByMessageInWhenDeleteFeedbacks() {
		// given
		Chat chat = mock(Chat.class);

		// when
		feedbackService.deleteAll(chat);

		// then
		verify(feedbackCommandService).deleteFeedbacksByChat(chat);
	}
}
