package com.sofa.linkiving.domain.chat.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sofa.linkiving.domain.chat.dto.internal.MessageDto;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Sentiment;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.link.dto.response.LinkCardRes;

import io.swagger.v3.oas.annotations.media.Schema;

public record MessageRes(
	@Schema(description = "메시지 ID")
	Long id,

	@Schema(description = "메시지 내용")
	String content,

	@Schema(description = "발신자 타입 (USER / AI)")
	Type type,

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(description = "피드백 상태 (AI 메시지인 경우만 포함: LIKE, DISLIKE, NONE)")
	Sentiment feedback,

	@Schema(description = "메시지 생성 시간", example = "2024-12-31 14:30:00")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
	LocalDateTime time,

	@Schema(description = "첨부된 링크 목록")
	List<LinkCardRes> links
) {
	public static MessageRes from(MessageDto messageDto) {
		Message message = messageDto.message();

		return new MessageRes(
			message.getId(),
			message.getContent(),
			message.getType(),
			message.getSentimentOrDefault(),
			message.getCreatedAt(),
			messageDto.linkDtos().stream()
				.map(LinkCardRes::from)
				.toList()
		);
	}
}
