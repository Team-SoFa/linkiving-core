package com.sofa.linkiving.domain.chat.service;

import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.repository.FeedbackRepository;

@ExtendWith(MockitoExtension.class)
public class FeedbackCommandServiceTest {
	@InjectMocks
	private FeedbackCommandService feedbackCommandService;

	@Mock
	private FeedbackRepository feedbackRepository;

	@Test
	@DisplayName("FeedbackRepository.deleteAllByMessageInQuery 호출")
	void shouldCallDeleteAllByMessageInQueryWhenDeleteAllByMessageIn() {
		// given
		Chat chat = mock(Chat.class); // List<Message> 대신 Chat 모킹

		// when
		feedbackCommandService.deleteFeedbacksByChat(chat);

		// then
		verify(feedbackRepository).deleteAllByChat(chat);
	}
}
