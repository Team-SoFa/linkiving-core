package com.sofa.linkiving.domain.chat.entity;

import java.util.ArrayList;
import java.util.List;

import com.sofa.linkiving.domain.chat.enums.Type;
import com.sofa.linkiving.global.common.BaseEntity;
import com.sofa.linkiving.global.converter.LongListToStringConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "messages")
public class Message extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chat_id", nullable = false)
	private Chat chat;

	@Column(nullable = false)
	private Type type;

	@Column(columnDefinition = "text", nullable = false)
	private String content;

	@Convert(converter = LongListToStringConverter.class)
	private List<Long> linkIds = new ArrayList<>();

	@OneToOne(mappedBy = "message")
	private Feedback feedback;

	@Builder
	public Message(Chat chat, Type type, String content, List<Long> linkIds) {
		this.chat = chat;
		this.type = type;
		this.content = content;
		this.linkIds = linkIds;
	}
}
