package com.sofa.linkiving.domain.chat.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.repository.MemberRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
public class ChatRepositoryTest {

	@Autowired
	private ChatRepository chatRepository;

	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private MessageRepository messageRepository;
	@Autowired
	private EntityManager em;

	private Member member;

	@BeforeEach
	void setUp() {
		member = memberRepository.save(Member.builder()
			.email("test@example.com")
			.password("test")
			.build());

		em.flush();
		em.clear();
	}

	@Test
	@DisplayName("빈 채팅방도 조회되며, 마지막 메시지 또는 생성 시각 기준으로 최신순 정렬됨")
	void shouldReturnChatsIncludingEmptyRoomsOrderByLastActivity() throws InterruptedException {
		// given

		// 메시지 없는 채팅방 -> 생성 시각 기준으로 함께 조회되어야 함
		chatRepository.save(Chat
			.builder()
			.member(member)
			.title("No Msg Chat")
			.build());

		Thread.sleep(100);

		// 오래된 메시지가 있는 채팅방
		Chat chatOldMsg = chatRepository.save(Chat.builder()
			.member(member)
			.title("Old Msg Chat")
			.build());
		messageRepository.save(Message.builder()
			.chat(chatOldMsg)
			.content("Old")
			.type(Type.USER)
			.build());

		Thread.sleep(100);

		// 최신 메시지가 있는 채팅방
		Chat chatNewMsg = chatRepository.save(Chat.builder()
			.member(member)
			.title("New Msg Chat")
			.build());
		messageRepository.save(Message
			.builder()
			.chat(chatNewMsg)
			.content("New")
			.type(Type.USER)
			.build());

		// when
		List<Chat> result = chatRepository.findAllByMemberOrderByLastMessageDesc(member);

		// then
		assertThat(result).hasSize(3);
		assertThat(result.get(0).getTitle()).isEqualTo("New Msg Chat");
		assertThat(result.get(1).getTitle()).isEqualTo("Old Msg Chat");
		assertThat(result.get(2).getTitle()).isEqualTo("No Msg Chat");
	}

	@Test
	@DisplayName("내 채팅방 조회 시 정상적으로 반환되어야 한다")
	void shouldReturnChatWhenMyChatExists() {
		// given
		Member me = memberRepository.save(
			Member.builder()
				.email("me@test.com")
				.password("password")
				.build());

		Chat myChat = chatRepository.save(
			Chat.builder()
				.member(me)
				.title("test")
				.build());

		// when
		Optional<Chat> result = chatRepository.findByIdAndMember(myChat.getId(), me);

		// then
		assertThat(result).isPresent();
	}

	@Test
	@DisplayName("다른 사람의 채팅방 조회 시 Empty를 반환해야 한다")
	void shouldReturnEmptyWhenChatIsNotMine() {
		// given
		Member me = memberRepository.save(
			Member.builder()
				.email("me@test.com")
				.password("password")
				.build());
		Member other = memberRepository.save(
			Member.builder()
				.email("other@test.com")
				.password("password")
				.build());

		Chat othersChat = chatRepository.save(
			Chat.builder()
				.member(other)
				.title("test")
				.build());

		// when: 내 정보(me)로 남의 채팅방(othersChat) 조회 시도
		Optional<Chat> result = chatRepository.findByIdAndMember(othersChat.getId(), me);

		// then
		assertThat(result).isEmpty(); // 조회되면 안 됨 (보안 검증)
	}

	@Test
	@DisplayName("내 채팅방인 경우 true 반환")
	void shouldReturnTrue_WhenChatExistsAndBelongsToMember() {
		// given
		Member me = memberRepository.save(Member.builder()
			.email("me@test.com")
			.password("password")
			.build());

		Chat myChat = chatRepository.save(Chat.builder()
			.member(me)
			.title("My Chat")
			.build());

		// when
		boolean exists = chatRepository.existsByIdAndMember(myChat.getId(), me);

		// then
		assertThat(exists).isTrue();
	}

	@Test
	@DisplayName("다른 사람의 채팅방인 경우 false 반환")
	void shouldReturnFalse_WhenChatBelongsToOtherMember() {
		// given
		Member me = memberRepository.save(Member.builder()
			.email("me@test.com")
			.password("password")
			.build());

		Member other = memberRepository.save(Member.builder()
			.email("other@test.com")
			.password("password")
			.build());

		Chat othersChat = chatRepository.save(Chat.builder()
			.member(other)
			.title("Other's Chat")
			.build());

		// when
		boolean exists = chatRepository.existsByIdAndMember(othersChat.getId(), me);

		// then
		assertThat(exists).isFalse();
	}

	@Test
	@DisplayName(" 존재하지 않는 채팅방 ID인 경우 false 반환")
	void shouldReturnFalse_WhenChatDoesNotExist() {
		// given
		Member me = memberRepository.save(Member.builder()
			.email("me@test.com")
			.password("password")
			.build());

		Long nonExistentChatId = 9999L;

		// when
		boolean exists = chatRepository.existsByIdAndMember(nonExistentChatId, me);

		// then
		assertThat(exists).isFalse();
	}
}
