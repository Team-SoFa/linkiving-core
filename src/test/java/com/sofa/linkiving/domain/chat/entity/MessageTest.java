package com.sofa.linkiving.domain.chat.entity;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.chat.enums.Type;
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

		List<Long> linkIds = List.of(1L, 100L, 500L);
		String content = "테스트 메시지입니다.";

		Message message = Message.builder()
			.chat(chat)
			.type(Type.AI)
			.content(content)
			.linkIds(linkIds)
			.build();

		// when
		Message savedMessage = em.persistFlushFind(message);

		// then
		assertThat(savedMessage).isNotNull();
		assertThat(savedMessage.getContent()).isEqualTo(content);
		assertThat(savedMessage.getChat()).isEqualTo(chat);

		// Converter 동작 검증
		assertThat(savedMessage.getLinkIds()).hasSize(3)
			.containsExactly(1L, 100L, 500L);
	}
}
