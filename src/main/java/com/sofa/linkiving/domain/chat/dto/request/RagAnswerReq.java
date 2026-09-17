package com.sofa.linkiving.domain.chat.dto.request;

import java.time.ZoneId;
import java.util.List;

import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Mode;
import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;
import com.sofa.linkiving.domain.link.enums.SummaryStatus;

public record RagAnswerReq(
	Long userId,
	String question,
	List<RagMessageReq> history,
	Mode mode
) {
	public static RagAnswerReq of(Long userId, String question, List<Message> messages, Mode mode) {
		List<RagMessageReq> history = messages.stream()
			.map(RagMessageReq::from)
			.toList();

		return new RagAnswerReq(userId, question, history, mode);
	}

	public record RagMessageReq(
		String role,
		String content,
		Long messageId,
		List<RagHistoryLinkReq> links,
		boolean linkOrderKnown,
		boolean linksTruncated
	) {
		public RagMessageReq(String role, String content) {
			this(role, content, null, List.of(), false, false);
		}

		public static RagMessageReq from(Message message) {
			return from(message, List.of(), false);
		}

		public static RagMessageReq from(Message message, List<RagHistoryLinkReq> links, boolean truncated) {
			String role = (message.getType() == Type.AI) ? "assistant" : "user";
			// message_link에는 순서가 저장되지 않아 과거 카드 순서를 추정하지 않는다.
			return new RagMessageReq(role, truncate(message.getContent(), 1500), message.getId(),
				List.copyOf(links), false, truncated);
		}
	}

	public record RagHistoryLinkReq(
		Long linkId, String title, String url, String summary, String memo, String savedAt, SummaryStatus summaryStatus
	) {
		public static RagHistoryLinkReq from(Link link, Summary summary) {
			// 운영 DB의 감사 시각은 Asia/Seoul 기준 LocalDateTime이다.
			String savedAt = link.getCreatedAt() == null ? null
				: link.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime().toString();
			return new RagHistoryLinkReq(link.getId(), truncate(link.getTitle(), 200), link.getUrl(),
				summary == null ? null : truncate(summary.getContent(), 800), truncate(link.getMemo(), 300),
				savedAt, link.getSummaryStatus());
		}
	}

	private static String truncate(String value, int limit) {
		if (value == null || value.codePointCount(0, value.length()) <= limit) {
			return value;
		}
		return value.substring(0, value.offsetByCodePoints(0, limit)) + "…";
	}
}
