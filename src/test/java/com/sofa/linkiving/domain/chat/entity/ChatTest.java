package com.sofa.linkiving.domain.chat.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.member.entity.Member;

@DataJpaTest
@ActiveProfiles("test")
public class ChatTest {

	@Autowired
	private TestEntityManager em;

	@Test
	@DisplayName("채팅방 생성 시 멤버 연관관계 정상 매핑")
	void shouldSaveChatWithMember() {
		// given
		Member member = Member.builder()
			.email("test@test.com")
			.password("password")
			.build();
		em.persist(member);

		Chat chat = Chat.builder()
			.member(member)
			.build();

		// when
		Chat savedChat = em.persistFlushFind(chat);

		// then
		assertThat(savedChat).isNotNull();
		assertThat(savedChat.getMember()).isEqualTo(member);
		assertThat(savedChat.getMember().getEmail()).isEqualTo("test@test.com");
	}
}
