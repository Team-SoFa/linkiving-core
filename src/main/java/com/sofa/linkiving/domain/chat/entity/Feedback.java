package com.sofa.linkiving.domain.chat.entity;

import com.sofa.linkiving.domain.chat.enums.Preference;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "feedbacks")
public class Feedback extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "message_id", nullable = false)
	private Message message;

	@Column(columnDefinition = "text", nullable = false)
	private String text;

	private Preference preference;

}
