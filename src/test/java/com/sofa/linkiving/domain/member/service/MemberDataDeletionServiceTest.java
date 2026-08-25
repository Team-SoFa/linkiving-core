package com.sofa.linkiving.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofa.linkiving.domain.chat.repository.ChatRepository;
import com.sofa.linkiving.domain.chat.repository.FeedbackRepository;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.link.repository.SummaryDeadLetterRepository;
import com.sofa.linkiving.domain.link.repository.SummaryRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.MemberStatus;
import com.sofa.linkiving.domain.report.repository.ReportRepository;
import com.sofa.linkiving.global.error.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class MemberDataDeletionServiceTest {

	@Mock FeedbackRepository feedbackRepository;
	@Mock MessageRepository messageRepository;
	@Mock ChatRepository chatRepository;
	@Mock SummaryRepository summaryRepository;
	@Mock SummaryDeadLetterRepository summaryDeadLetterRepository;
	@Mock LinkRepository linkRepository;
	@Mock ReportRepository reportRepository;
	@Mock MemberCommandService memberCommandService;
	@Mock MemberQueryService memberQueryService;
	@InjectMocks MemberDataDeletionService memberDataDeletionService;

	@Test
	void shouldDeleteDependentsBeforeMember() {
		Member member = Member.builder().email("member@test.com").build();
		ReflectionTestUtils.setField(member, "id", 1L);
		member.beginWithdrawal();
		given(memberQueryService.getUserForUpdate(member.getEmail())).willReturn(member);

		memberDataDeletionService.deleteAll(1L, member.getEmail());

		InOrder order = inOrder(feedbackRepository, messageRepository, chatRepository, summaryRepository,
			summaryDeadLetterRepository, linkRepository, reportRepository, memberCommandService);
		order.verify(feedbackRepository).deleteAllByMemberId(1L);
		order.verify(messageRepository).deleteLinkMappingsByMemberId(1L);
		order.verify(messageRepository).deleteAllByMemberId(1L);
		order.verify(chatRepository).deleteAllByMemberId(1L);
		order.verify(summaryRepository).deleteAllByMemberId(1L);
		order.verify(summaryDeadLetterRepository).deleteAllByMemberId(1L);
		order.verify(linkRepository).deleteAllByMemberId(1L);
		order.verify(reportRepository).deleteAllByMemberId(1L);
		order.verify(memberCommandService).delete(member);
	}

	@Test
	void shouldRejectMismatchedMemberIdentity() {
		Member member = Member.builder().email("member@test.com").build();
		ReflectionTestUtils.setField(member, "id", 2L);
		member.beginWithdrawal();
		given(memberQueryService.getUserForUpdate(member.getEmail())).willReturn(member);

		assertThatThrownBy(() -> memberDataDeletionService.deleteAll(1L, member.getEmail()))
			.isInstanceOf(BusinessException.class);

		verify(memberCommandService, never()).delete(member);
	}

	@Test
	void shouldPersistWithdrawingStateBeforeExternalDeletion() {
		Member member = Member.builder().email("member@test.com").status(MemberStatus.ACTIVE).build();
		ReflectionTestUtils.setField(member, "id", 1L);
		given(memberQueryService.getUserForUpdate(member.getEmail())).willReturn(member);

		memberDataDeletionService.beginWithdrawal(1L, member.getEmail());

		org.assertj.core.api.Assertions.assertThat(member.isWithdrawing()).isTrue();
	}
}
