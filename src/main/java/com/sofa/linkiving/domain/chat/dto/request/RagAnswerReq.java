package com.sofa.linkiving.domain.chat.dto.request;

import java.util.List;

import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.chat.enums.Mode;
import com.sofa.linkiving.domain.chat.enums.Type;

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
		String content
	) {
		public static RagMessageReq from(Message message) {
			String role = (message.getType() == Type.AI) ? "system" : "user";
			return new RagMessageReq(role, message.getContent());
		}
	}
}
