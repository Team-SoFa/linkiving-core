package com.sofa.linkiving.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;
import com.sofa.linkiving.domain.link.entity.Link;

@ExtendWith(MockitoExtension.class)
public class MessageCommandServiceTest {

	@InjectMocks
	private MessageCommandService messageCommandService;

	@Mock
	private MessageRepository messageRepository;

	@Test
	@DisplayName("MessageRepository.save 호출 및 저장된 Message 반환")
	void shouldReturnSavedMessageWhenSaveMessage() {
		// given
		Message message = mock(Message.class);
		given(messageRepository.save(any(Message.class))).willReturn(message);

		// when
		Message result = messageCommandService.saveMessage(message);

		// then
		assertThat(result).isEqualTo(message);
		verify(messageRepository).save(message);
	}

	@Test
	@DisplayName("MessageRepository.deleteAllByChat 호출")
	void shouldCallDeleteAllByChatWhenDeleteAllByChat() {
		// given
		Chat chat = mock(Chat.class);

		// when
		messageCommandService.deleteAllByChat(chat);

		// then
		verify(messageRepository).deleteAllByChat(chat);
	}

	@Test
	@DisplayName("USER 타입의 메시지를 생성하고 저장한다")
	void shouldSaveUserMessageCorrectly() {
		// given
		Chat chat = mock(Chat.class);
		String content = "유저 질문";

		// save 호출 시 입력된 객체를 그대로 반환하도록 설정
		given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		Message savedMessage = messageCommandService.saveUserMessage(chat, content);

		// then
		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(messageRepository).save(captor.capture());

		Message captured = captor.getValue();
		assertThat(captured.getChat()).isEqualTo(chat);
		assertThat(captured.getContent()).isEqualTo(content);
		assertThat(captured.getType()).isEqualTo(Type.USER);
	}

	@Test
	@DisplayName("AI 타입의 메시지와 링크 정보를 저장한다")
	void shouldSaveAiMessageCorrectly() {
		// given
		Chat chat = mock(Chat.class);
		String content = "AI 답변";
		List<Link> links = List.of(mock(Link.class));

		given(messageRepository.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		Message savedMessage = messageCommandService.saveAiMessage(chat, content, links);

		// then
		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(messageRepository).save(captor.capture());

		Message captured = captor.getValue();
		assertThat(captured.getChat()).isEqualTo(chat);
		assertThat(captured.getContent()).isEqualTo(content);
		assertThat(captured.getLinks()).isEqualTo(links);
		assertThat(captured.getType()).isEqualTo(Type.AI);
	}
}
