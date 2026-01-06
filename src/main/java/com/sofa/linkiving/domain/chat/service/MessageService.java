package com.sofa.linkiving.domain.chat.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.chat.dto.internal.MessageDto;
import com.sofa.linkiving.domain.chat.dto.internal.MessagesDto;
import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.service.SummaryQueryService;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageService {
	private final MessageCommandService messageCommandService;
	private final MessageQueryService messageQueryService;
	private final SummaryQueryService summaryQueryService;

	public Message get(Long messageId, Member member) {
		return messageQueryService.findByIdAndMember(messageId, member);
	}

	public void deleteAll(Chat chat) {
		messageCommandService.deleteAllByChat(chat);
	}

	public MessagesDto getMessages(Chat chat, Long lastId, int size) {
		Slice<Message> messageSlice = messageQueryService.findAllByChatAndCursor(chat, lastId, size);
		List<Message> messages = messageSlice.getContent();

		List<Link> links = messages.stream()
			.flatMap(msg -> msg.getLinks().stream())
			.distinct()
			.toList();

		Map<Long, Summary> summaryMap = summaryQueryService.getSelectedSummariesByLinks(links);

		List<MessageDto> messageDtos = messages.stream()
			.map(msg -> {
				List<LinkDto> linkDtos = msg.getLinks().stream()
					.map(link -> new LinkDto(link, summaryMap.get(link.getId())))
					.toList();
				return new MessageDto(msg, linkDtos);
			})
			.toList();

		return new MessagesDto(messageDtos, messageSlice.hasNext());
	}
}
