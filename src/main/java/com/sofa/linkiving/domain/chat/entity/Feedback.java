package com.sofa.linkiving.domain.chat.entity;

import com.sofa.linkiving.domain.chat.enums.Sentiment;
import com.sofa.linkiving.global.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "feedbacks")
public class Feedback extends BaseEntity {
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "message_id")
	private Message message;

	@Column(columnDefinition = "text", nullable = false)
	private String text;

	@Column(nullable = false, columnDefinition = "SMALLINT")
	private Sentiment sentiment;

	@Builder
	public Feedback(Message message, String text, Sentiment sentiment) {
		this.message = message;
		this.text = text;
		this.sentiment = sentiment;
	}
}
