package com.sofa.linkiving.domain.chat.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Feedback;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Sentiment;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.repository.MemberRepository;

@DataJpaTest
@ActiveProfiles("test")
class MessageRepositoryTest {

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private ChatRepository chatRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private FeedbackRepository feedbackRepository;

	@Autowired
	private TestEntityManager em;

	private Chat chat;
	private Member member;

	@BeforeEach
	void setUp() {
		member = memberRepository.save(Member.builder()
			.email("test@repo.com")
			.password("password")
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

		Message msg3 = messageRepository.save(Message.builder()
			.chat(chat)
			.content("3")
			.type(Type.USER)
			.build());

		List<Message> result = messageRepository.findAllByChatAndCursor(
			chat,
			msg3.getId(),
			PageRequest.of(0, 10)
		);

		// then
		assertThat(result).hasSize(2);
		// 최신 순 정렬 확인
		assertThat(result.get(0).getContent()).isEqualTo("2");
		assertThat(result.get(1).getContent()).isEqualTo("1");
	}

	@Test
	@DisplayName("메시지 조회 시 연관된 링크도 정상적으로 조회됨")
	void shouldReturnMessageWithLinks() {
		// given
		Link link1 = Link.builder()
			.member(member)
			.title("Naver")
			.url("https://naver.com")
			.imageUrl("img1.png")
			.build();

		Link link2 = Link.builder()
			.member(member)
			.title("Google")
			.url("https://google.com")
			.imageUrl("img2.png")
			.build();

		em.persist(link1);
		em.persist(link2);

		Message message = Message.builder()
			.chat(chat)
			.content("Check links")
			.type(Type.AI)
			.links(List.of(link1, link2))
			.build();

		messageRepository.save(message);

		em.flush();
		em.clear();

		// when
		List<Message> result = messageRepository.findAllByChatAndCursor(chat, null, PageRequest.of(0, 10));

		// then
		assertThat(result).hasSize(1);
		Message fetchedMessage = result.get(0);

		// Link 데이터가 정상 로딩 확인
		assertThat(fetchedMessage.getLinks()).hasSize(2);
		assertThat(fetchedMessage.getLinks())
			.extracting("title")
			.containsExactlyInAnyOrder("Naver", "Google");
	}

	@Test
	@DisplayName("메시지 조회 시 연관된 피드백도 함께 조회됨 (Fetch Join)")
	void shouldReturnMessageWithFeedback() {
		// given
		Message message = messageRepository.save(Message.builder()
			.chat(chat)
			.content("AI Reply")
			.type(Type.AI)
			.build());

		feedbackRepository.save(Feedback.builder()
			.message(message)
			.text("Good")
			.sentiment(Sentiment.LIKE)
			.build());

		em.flush();
		em.clear();

		// when
		List<Message> result = messageRepository.findAllByChatAndCursor(chat, null, PageRequest.of(0, 10));

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getFeedback()).isNotNull();
		assertThat(result.get(0).getFeedback().getText()).isEqualTo("Good");
	}
}
