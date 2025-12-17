package com.sofa.linkiving.domain.chat.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Feedback;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Sentiment;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.repository.MemberRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
public class MessageRepositoryTest {

	@Autowired
	private MessageRepository messageRepository;
	@Autowired
	private ChatRepository chatRepository;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private FeedbackRepository feedbackRepository;
	@Autowired
	private EntityManager em;

	private Chat chat;

	@BeforeEach
	void setUp() {
		Member member = memberRepository.save(Member.builder()
			.email("test@example.com")
			.password("password123")
			.build());

		chat = chatRepository.save(Chat.builder()
			.member(member)
			.title("Test Chat")
			.build());

		em.flush();
		em.clear();
	}

	@Test
	@DisplayName("채팅방 메시지 커서 기반 조회: 첫 페이지 (lastId가 null일 때)")
	void shouldReturnLatestMessagesWhenLastIdIsNull() {
		// given
		for (int i = 1; i <= 30; i++) {
			messageRepository.save(Message.builder()
				.chat(chat)
				.content("Msg " + i)
				.type(Type.USER)
				.build());
		}

		// when: lastId = null, size = 10
		List<Message> result = messageRepository.findAllByChatAndCursor(
			chat,
			null,
			PageRequest.of(0, 10)
		);

		// then
		assertThat(result).hasSize(10);
		// 최신순 정렬이므로 30, 29, ..., 21 순서여야 함
		assertThat(result.get(0).getContent()).isEqualTo("Msg 30");
		assertThat(result.get(9).getContent()).isEqualTo("Msg 21");
	}

	@Test
	@DisplayName("채팅방 메시지 커서 기반 조회: 다음 페이지 (lastId 지정 시)")
	void shouldReturnMessagesBeforeLastId() {
		// given

		messageRepository.save(Message.builder()
			.chat(chat)
			.content("1")
			.type(Type.USER)
			.build());

		messageRepository.save(Message.builder()
			.chat(chat)
			.content("2")
			.type(Type.USER)
			.build());

		Message msg = messageRepository.save(Message.builder()
			.chat(chat)
			.content("3")
			.type(Type.USER)
			.build());

		// when: lastId = msg3.getId() (3번 메시지 이전의 데이터를 조회)
		List<Message> result = messageRepository.findAllByChatAndCursor(
			chat,
			msg.getId(),
			PageRequest.of(0, 10)
		);

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getContent()).isEqualTo("2");
		assertThat(result.get(1).getContent()).isEqualTo("1");
	}

	@Test
	@DisplayName("메시지 조회 시 연관된 피드백이 Fetch Join으로 함께 조회됨")
	void shouldReturnMessageWithFeedbackWhenExists() {
		// given
		Message message = messageRepository.save(Message.builder()
			.chat(chat)
			.content("AI Reply")
			.type(Type.AI)
			.build());

		feedbackRepository.save(Feedback.builder()
			.message(message)
			.text("Good Response")
			.sentiment(Sentiment.LIKE)
			.build());

		em.flush();
		em.clear();

		// when
		List<Message> result = messageRepository.findAllByChatAndCursor(
			chat,
			null,
			PageRequest.of(0, 10)
		);

		// then
		assertThat(result).hasSize(1);
		Message fetchedMessage = result.get(0);

		assertThat(fetchedMessage.getFeedback()).isNotNull();
		assertThat(fetchedMessage.getFeedback().getText()).isEqualTo("Good Response");
		assertThat(fetchedMessage.getFeedback().getSentiment()).isEqualTo(Sentiment.LIKE);
	}
}
