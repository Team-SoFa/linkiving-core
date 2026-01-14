package com.sofa.linkiving.domain.chat.dto.response;

import java.util.List;

import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.dto.response.LinkCardRes;

import io.swagger.v3.oas.annotations.media.Schema;

public record AnswerRes(
	@Schema(description = "성공 여부")
	Boolean success,
	@Schema(description = "채팅방 ID")
	Long chatId,
	@Schema(description = "메세지 ID")
	Long messageId,
	@Schema(description = "답변 내용")
	String content,
	@Schema(description = "스텝 목록")
	List<String> step,
	@Schema(description = "첨부된 링크 목록")
	List<LinkCardRes> links
) {
	public static AnswerRes of(Long chatId, Message message, List<String> step, List<LinkDto> linkDtos) {
		return new AnswerRes(
			true,
			chatId,
			message.getId(),
			message.getContent(),
			step,
			linkDtos.stream().map(LinkCardRes::from).toList()
		);
	}

	public static AnswerRes error(Long chatId, String content) {
		return new AnswerRes(
			false,
			chatId,
			null,
			content,
			null,
			null
		);
	}
}
