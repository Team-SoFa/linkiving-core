package com.sofa.linkiving.domain.chat.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofa.linkiving.domain.chat.dto.internal.RagHistoryLinkRow;
import com.sofa.linkiving.domain.chat.dto.request.RagAnswerReq.RagMessageReq;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.member.entity.Member;

class RagHistoryServiceTest {

	private final MessageRepository repository = mock(MessageRepository.class);
	private final RagHistoryService service = new RagHistoryService(repository);
	private final Member member = mock(Member.class);
	private final Chat chat = mock(Chat.class);

	@Test
	void limitsContextAndKeepsChronologyWithoutInventingHistoricalRank() throws Exception {
		List<Message> messages = new ArrayList<>();
		List<RagHistoryLinkRow> rows = new ArrayList<>();
		for (long id = 7; id >= 1; id--) {
			messages.add(message(id, Type.AI));
			for (long index = 1; index <= 12; index++) {
				Link link = Link.builder().member(member).url("https://example.com/" + index)
					.title("제목").memo("메".repeat(400)).build();
				ReflectionTestUtils.setField(link, "id", id * 100 + index);
				ReflectionTestUtils.setField(link, "createdAt", LocalDateTime.of(2026, 8, 17, 0, 30));
				Summary summary = Summary.builder().link(link).content("😀".repeat(900)).selected(true).build();
				rows.add(new RagHistoryLinkRow(id, link, summary));
			}
		}
		when(repository.findRagHistory(chat, member, 8L, PageRequest.of(0, 7))).thenReturn(List.copyOf(messages));
		when(repository.findRagHistoryLinks(anyList(), eq(chat), eq(member))).thenReturn(rows);

		List<RagMessageReq> history = service.findHistory(8L, chat, member);

		assertThat(history).extracting(RagMessageReq::messageId).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L);
		assertThat(history.stream().mapToInt(item -> item.links().size()).sum()).isEqualTo(20);
		assertThat(history.get(6).links()).hasSize(10);
		assertThat(history.get(5).links()).hasSize(10);
		assertThat(history.get(4).links()).isEmpty();
		assertThat(history).allSatisfy(item -> {
			assertThat(item.role()).isEqualTo("assistant");
			assertThat(item.linkOrderKnown()).isFalse();
			assertThat(item.linksTruncated()).isTrue();
			assertThat(item.content()).hasSize(1501).endsWith("…");
		});
		var context = history.get(6).links().get(0);
		assertThat(context.savedAt()).isEqualTo("2026-08-17T00:30+09:00");
		assertThat(context.memo()).hasSize(301);
		assertThat(context.summary().codePointCount(0, context.summary().length())).isEqualTo(801);
		assertThat(context.summary()).endsWith("😀…");
		var json = new ObjectMapper().valueToTree(history.get(6));
		assertThat(json.get("linkOrderKnown").isBoolean()).isTrue();
		assertThat(json.get("links").get(0).has("linkId")).isTrue();
		verify(repository, times(1)).findRagHistoryLinks(anyList(), eq(chat), eq(member));
	}

	@Test
	void userOnlyHistoryDoesNotFetchLinks() {
		when(repository.findRagHistory(chat, member, 2L, PageRequest.of(0, 7)))
			.thenReturn(List.of(message(1L, Type.USER)));
		var history = service.findHistory(2L, chat, member);
		assertThat(history.get(0).role()).isEqualTo("user");
		assertThat(history.get(0).links()).isEmpty();
		verify(repository, never()).findRagHistoryLinks(anyList(), any(), any());
	}

	@Test
	void emptyHistoryDoesNotFetchLinks() {
		when(repository.findRagHistory(any(), any(), any(), any())).thenReturn(List.of());
		assertThat(service.findHistory(1L, chat, member)).isEmpty();
		verify(repository, never()).findRagHistoryLinks(anyList(), any(), any());
	}

	private Message message(Long id, Type type) {
		Message message = Message.builder().chat(chat).type(type).content("답".repeat(1600)).build();
		ReflectionTestUtils.setField(message, "id", id);
		return message;
	}
}
