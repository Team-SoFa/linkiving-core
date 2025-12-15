package com.sofa.linkiving.domain.chat.service;

import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.entity.Chat;

@ExtendWith(MockitoExtension.class)
public class FeedbackServiceTest {
	@InjectMocks
	private FeedbackService feedbackService;

	@Mock
	private FeedbackCommandService feedbackCommandService;

	@Mock
	private FeedbackQueryService feedbackQueryService;

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

