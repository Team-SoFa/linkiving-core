package com.sofa.linkiving.domain.chat.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Sentiment;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.link.entity.Link;

import io.swagger.v3.oas.annotations.media.Schema;

public record MessagesRes(
	@Schema(description = "메시지 목록")
	List<MessageDto> messages,

	@Schema(description = "다음 페이지 존재 여부")
	boolean hasNext,

	@Schema(description = "마지막 메시지 ID (다음 요청 커서용)")
	Long lastId
) {
	public static MessagesRes of(List<Message> messages, boolean hasNext) {
		List<MessageDto> messageDtos = messages.stream()
			.map(MessageDto::from)
			.toList();

		Long lastId = messages.isEmpty() ? null : messages.get(messages.size() - 1).getId();

		return new MessagesRes(messageDtos, hasNext, lastId);
	}

	public record MessageDto(
		@Schema(description = "메시지 ID")
		Long id,

		@Schema(description = "메시지 내용")
		String content,

		@Schema(description = "발신자 타입 (USER / AI)")
		Type type,

		@JsonInclude(JsonInclude.Include.NON_NULL)
		@Schema(description = "피드백 상태 (AI 메시지인 경우만 포함: LIKE, DISLIKE, NONE)")
		Sentiment feedback,

		@Schema(description = "첨부된 링크 목록")
		List<LinkPreviewDto> links
	) {
		public static MessageDto from(Message message) {
			Sentiment feedbackStatus = null;

			if (message.getType() == Type.AI) {
				feedbackStatus = (message.getFeedback() != null)
					? message.getFeedback().getSentiment()
					: Sentiment.NONE;
			}

			// [변경] 엔티티에서 링크 목록을 바로 변환
			List<LinkPreviewDto> linkDtos = message.getLinks().stream()
				.map(LinkPreviewDto::from)
				.toList();

			return new MessageDto(
				message.getId(),
				message.getContent(),
				message.getType(),
				feedbackStatus,
				linkDtos
			);
		}
	}

	public record LinkPreviewDto(
		Long id,
		String title,
		String url,
		String imageUrl
	) {
		public static LinkPreviewDto from(Link link) {
			return new LinkPreviewDto(
				link.getId(),
				link.getTitle(),
				link.getUrl(),
				link.getImageUrl()
			);
		}
	}
}
