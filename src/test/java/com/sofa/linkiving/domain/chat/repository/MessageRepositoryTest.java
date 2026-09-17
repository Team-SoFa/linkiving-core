package com.sofa.linkiving.domain.chat.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.hibernate.SessionFactory;
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
import com.sofa.linkiving.domain.chat.service.RagHistoryService;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.repository.MemberRepository;

@DataJpaTest(properties = {"test.external.base-url=http://127.0.0.1:1", "ai.server.url=http://127.0.0.1:1",
	"spring.jpa.properties.hibernate.generate_statistics=true"})
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
			.build());

		chat = chatRepository.save(Chat.builder()
			.member(member)
			.title("Test Chat")
			.build());

		em.flush();
		em.clear();
	}

	@Test
	void ragHistoryFiltersForeignAndDeletedLinksAndMapsBeforeDetaching() {
		Member other = memberRepository.save(Member.builder().email("other@example.com").build());
		Link owned = em.persist(Link.builder().member(member).title("owned").url("https://example.com/1").build());
		Link foreign = em.persist(Link.builder().member(other).title("foreign").url("https://example.com/2").build());
		Link deleted = em.persist(Link.builder().member(member).title("deleted").url("https://example.com/3").build());
		deleted.markDeleted();
		Message visible = messageRepository.save(Message.builder().chat(chat).content("answer").type(Type.AI)
			.links(List.of(foreign, deleted, owned)).build());
		Message removed = messageRepository.save(Message.builder().chat(chat).content("removed").type(Type.AI).build());
		removed.markDeleted();
		Message question = messageRepository.save(Message.builder().chat(chat).content("follow-up")
			.type(Type.USER).build());
		em.flush();
		em.clear();

		var statistics = em.getEntityManager().getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		statistics.clear();
		var history = new RagHistoryService(messageRepository).findHistory(question.getId(), chat, member);
		assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
		em.clear();

		assertThat(history).hasSize(1);
		assertThat(history.get(0).messageId()).isEqualTo(visible.getId());
		assertThat(history.get(0).links()).extracting("linkId").containsExactly(owned.getId());
		assertThat(history.get(0).links().get(0).summary()).isNull();
		assertThat(messageRepository.findRagHistory(chat, other, question.getId(), PageRequest.of(0, 7))).isEmpty();
		assertThat(messageRepository.findRagHistoryLinks(List.of(visible.getId()), chat, other)).isEmpty();
	}

	@Test
	void ragHistoryLimitsMessagesAtDatabaseQuery() {
		for (int i = 0; i < 12; i++) {
			messageRepository.save(Message.builder().chat(chat).content("message " + i).type(Type.USER).build());
		}
		em.flush();
		assertThat(messageRepository.findRagHistory(chat, member, Long.MAX_VALUE, PageRequest.of(0, 7)))
			.hasSize(7).extracting(Message::getContent)
			.containsExactly("message 11", "message 10", "message 9", "message 8", "message 7", "message 6",
				"message 5");
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

	@Test
	@DisplayName(" 특정 ID보다 작은 메시지 중 최신 7개를 내림차순으로 조회")
	void shouldReturnTop7MessagesBeforeGivenIdDesc() {
		// given
		for (int i = 1; i <= 15; i++) {
			messageRepository.save(Message.builder()
				.chat(chat)
				.content("Message " + i)
				.type(Type.USER)
				.build());
		}

		List<Message> allMessages = messageRepository.findAll();
		Long targetId = allMessages.get(10).getId();

		// when
		List<Message> result = messageRepository.findTop7ByChatAndIdLessThanOrderByIdDesc(chat, targetId);

		// then
		assertThat(result).hasSize(7);
		assertThat(result).allMatch(msg -> msg.getId() < targetId);
		assertThat(result.get(0).getId()).isGreaterThan(result.get(1).getId());
		assertThat(result.get(0).getContent()).isEqualTo(allMessages.get(9).getContent()); // ID 10
		assertThat(result.get(6).getContent()).isEqualTo(allMessages.get(3).getContent()); // ID 4
	}

	@Test
	@DisplayName(" 조건에 맞는 메시지가 7개 미만이면 전체 반환")
	void shouldReturnAllMessages_WhenLessThan7() {
		// given
		for (int i = 1; i <= 5; i++) {
			messageRepository.save(Message.builder()
				.chat(chat)
				.content("Message " + i)
				.type(Type.USER)
				.build());
		}

		List<Message> allMessages = messageRepository.findAll();
		Long targetId = allMessages.get(4).getId() + 100L;

		// when
		List<Message> result = messageRepository.findTop7ByChatAndIdLessThanOrderByIdDesc(chat, targetId);

		// then
		assertThat(result).hasSize(5);
		assertThat(result.get(0).getId()).isGreaterThan(result.get(4).getId()); // 정렬 확인
	}
}
