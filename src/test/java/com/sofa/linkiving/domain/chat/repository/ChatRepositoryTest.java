package com.sofa.linkiving.domain.chat.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.repository.MemberRepository;

@DataJpaTest
@ActiveProfiles("test")
public class ChatRepositoryTest {
	@Autowired
	private ChatRepository chatRepository;
	@Autowired
	private MemberRepository memberRepository;

	@Test
	@DisplayName("회원별 채팅방 목록 조회 및 생성일 내림차순 정렬")
	void shouldReturnChatsDescByCreatedAtWhenFindAllByMember() throws InterruptedException {
		// given
		Member member = memberRepository.save(
			Member.builder()
				.email("test@list.com")
				.password("password")
				.build());

		Chat oldChat = chatRepository.save(Chat.builder()
			.member(member)
			.title("Old Chat")
			.build());
		Thread.sleep(100);
		Chat newChat = chatRepository.save(Chat.builder()
			.member(member)
			.title("New Chat")
			.build());

		// when
		List<Chat> result = chatRepository.findAllByMemberOrderByCreatedAtDesc(member);

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getTitle()).isEqualTo("New Chat");
		assertThat(result.get(1).getTitle()).isEqualTo("Old Chat");
	}
}
