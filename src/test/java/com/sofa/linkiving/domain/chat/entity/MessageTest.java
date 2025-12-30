package com.sofa.linkiving.domain.chat.entity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.chat.enums.Sentiment;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.member.entity.Member;

@DataJpaTest
@ActiveProfiles("test")
public class MessageTest {

	@Autowired
	private TestEntityManager em;

	@Test
	@DisplayName("메시지 저장 시 내용 및 링크 ID 목록 정상 저장 및 조회")
	void shouldSaveMessageWithContentAndLinkIds() {
		// given
		Member member = Member
			.builder()
			.email("test@test.com")
			.password("password")
			.build();
		em.persist(member);

		Chat chat = Chat.builder()
			.member(member)
			.title("test")
			.build();
		em.persist(chat);

		Link link1 = Link.builder()
			.member(member)
			.url("https://example1.com")
			.title("Link 1")
			.build();
		em.persist(link1);

		Link link2 = Link.builder()
			.member(member)
			.url("https://example2.com")
			.title("Link 2")
			.build();
		em.persist(link2);

		String content = "테스트 메시지입니다.";

		Message message = Message.builder()
			.chat(chat)
			.type(Type.AI)
			.content(content)
			.links(List.of(link1, link2))
			.build();

		// when
		Message savedMessage = em.persistFlushFind(message);

		// then
		assertThat(savedMessage).isNotNull();
		assertThat(savedMessage.getContent()).isEqualTo(content);
		assertThat(savedMessage.getChat()).isEqualTo(chat);

		// Converter 동작 검증
		assertThat(savedMessage.getLinks()).hasSize(2)
			.extracting("url")
			.containsExactlyInAnyOrder("https://example1.com", "https://example2.com");
	}

	@Test
	@DisplayName("메시지 타입이 AI가 아니면 null을 반환한다")
	void shouldReturnNullWhenTypeIsNotAi() {
		// given
		Message userMessage = Message.builder()
			.type(Type.USER)
			.content("안녕하세요")
			.build();

		// when
		Sentiment result = userMessage.getSentimentOrDefault();

		// then
		assertThat(result).isNull();
	}

	@Test
	@DisplayName("AI 메시지이지만 피드백이 없으면 Sentiment.NONE을 반환한다")
	void shouldReturnNoneWhenFeedbackIsNull() {
		// given
		Message aiMessage = Message.builder()
			.type(Type.AI)
			.content("답변입니다")
			.build();

		// when
		Sentiment result = aiMessage.getSentimentOrDefault();

		// then
		assertThat(result).isEqualTo(Sentiment.NONE);
	}

	@Test
	@DisplayName("AI 메시지이고 피드백이 존재하면 해당 피드백의 감정 상태를 반환한다")
	void shouldReturnFeedbackSentimentWhenExists() {
		// given
		Message aiMessage = Message.builder()
			.type(Type.AI)
			.content("답변입니다")
			.build();

		Feedback feedback = mock(Feedback.class);
		given(feedback.getSentiment()).willReturn(Sentiment.LIKE);

		ReflectionTestUtils.setField(aiMessage, "feedback", feedback);

		// when
		Sentiment result = aiMessage.getSentimentOrDefault();

		// then
		assertThat(result).isEqualTo(Sentiment.LIKE);
	}
}
