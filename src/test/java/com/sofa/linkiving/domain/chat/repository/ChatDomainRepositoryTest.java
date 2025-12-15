package com.sofa.linkiving.domain.chat.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Feedback;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Sentiment;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.repository.MemberRepository;

@DataJpaTest
@ActiveProfiles("test")
public class ChatDomainRepositoryTest {
	@Autowired
	private ChatRepository chatRepository;

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private FeedbackRepository feedbackRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	@DisplayName("ChatRepository: 내 채팅방 조회")
	void shouldFindChatByIdAndMember() {
		// given
		Member member = memberRepository.save(Member
			.builder()
			.email("test@test.com")
			.password("test")
			.build());

		Chat chat = chatRepository.save(Chat.builder()
			.member(member)
			.title("Title")
			.build());

		// when
		Optional<Chat> result = chatRepository.findByIdAndMember(chat.getId(), member);

		// then
		assertThat(result).isPresent();
		assertThat(result.get()).isEqualTo(chat);
	}

	@Test
	@DisplayName("MessageRepository: 채팅방의 모든 메시지 삭제")
	void shouldDeleteAllMessagesByChat() {
		// given
		Member member = memberRepository.save(Member.builder()
			.email("test@test.com")
			.password("test")
			.build());

		Chat chat = chatRepository.save(Chat.builder()
			.member(member)
			.title("Chat")
			.build());

		messageRepository.save(Message.builder()
			.chat(chat)
			.content("Hello")
			.type(Type.USER)
			.build());

		// when
		messageRepository.deleteAllByChat(chat);

		// then
		List<Message> remaining = messageRepository.findAllByChat(chat);
		assertThat(remaining).isEmpty();
	}

	@Test
	@DisplayName("FeedbackRepository: 메시지 목록에 포함된 피드백 일괄 삭제")
	void shouldDeleteAllFeedbacksByMessageList() {
		// given
		Member member = memberRepository.save(Member.builder()
			.email("feed@test.com")
			.password("test")
			.build());

		Chat chat = chatRepository.save(Chat.builder()
			.member(member)
			.title("Chat")
			.build());

		// 메시지 생성
		Message msg1 = messageRepository.save(Message.builder()
			.chat(chat)
			.content("1")
			.type(Type.AI)
			.build());

		Message msg2 = messageRepository.save(Message.builder()
			.chat(chat)
			.content("2")
			.type(Type.AI)
			.build());

		// 피드백 생성
		feedbackRepository.save(new Feedback(msg1, "Good", Sentiment.LIKE));
		feedbackRepository.save(new Feedback(msg2, "Bad", Sentiment.DISLIKE));

		// when
		feedbackRepository.deleteAllByChat(chat);

		// then
		assertThat(feedbackRepository.count()).isZero();
	}

	@Test
	@DisplayName("통합 검증: 메시지가 없는 빈 채팅방 삭제 시 정상 작동")
	void shouldDeleteEmptyChatWithoutError() {
		// given
		Member member = memberRepository.save(Member.builder()
			.email("empty@test.com")
			.password("test")
			.build());

		Chat emptyChat = chatRepository.save(Chat.builder()
			.member(member)
			.title("Empty Chat")
			.build());

		// when & then
		assertThatCode(() -> {
			feedbackRepository.deleteAllByChat(emptyChat);
			messageRepository.deleteAllByChat(emptyChat);
			chatRepository.delete(emptyChat);
		}).doesNotThrowAnyException();

		assertThat(chatRepository.existsById(emptyChat.getId())).isFalse();
	}
}
