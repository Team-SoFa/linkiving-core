package com.sofa.linkiving.domain.chat.entity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.chat.enums.Sentiment;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.member.entity.Member;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Feedback 엔티티 테스트")
public class FeedbackTest {

	@Autowired
	private TestEntityManager em;

	@Test
	@DisplayName("피드백 저장 시 메시지 및 상태 정상 저장")
	void shouldSaveFeedbackWithSentiment() {
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

		Message message = Message.builder()
			.chat(chat)
			.type(Type.USER)
			.content("답변입니다.")
			.links(Collections.emptyList())
			.build();
		em.persist(message);

		String feedbackText = "좋은 답변 감사합니다.";
		Sentiment sentiment = Sentiment.LIKE;

		Feedback feedback = Feedback.builder()
			.message(message)
			.text(feedbackText)
			.sentiment(sentiment)
			.build();

		// when
		Feedback savedFeedback = em.persistFlushFind(feedback);

		// then
		assertThat(savedFeedback).isNotNull();
		assertThat(savedFeedback.getMessage().getId()).isEqualTo(message.getId());
		assertThat(savedFeedback.getText()).isEqualTo(feedbackText);
		assertThat(savedFeedback.getSentiment()).isEqualTo(sentiment);
	}

	@Test
	@DisplayName("피드백 내용과 상태를 업데이트할 수 있다")
	void shouldUpdateFeedback() {
		// given
		Message mockMessage = mock(Message.class);
		Feedback feedback = Feedback.builder()
			.message(mockMessage)
			.text("기존 피드백 내용")
			.sentiment(Sentiment.LIKE)
			.build();

		// when
		feedback.update("수정된 피드백 내용", Sentiment.DISLIKE);

		// then
		assertThat(feedback.getText()).isEqualTo("수정된 피드백 내용");
		assertThat(feedback.getSentiment()).isEqualTo(Sentiment.DISLIKE);
	}
}
