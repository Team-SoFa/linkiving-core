package com.sofa.linkiving.domain.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.chat.repository.ChatRepository;
import com.sofa.linkiving.domain.chat.repository.FeedbackRepository;
import com.sofa.linkiving.domain.chat.repository.MessageRepository;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.link.repository.SummaryDeadLetterRepository;
import com.sofa.linkiving.domain.link.repository.SummaryRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.error.MemberErrorCode;
import com.sofa.linkiving.domain.report.repository.ReportRepository;
import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberDataDeletionService {

	private final FeedbackRepository feedbackRepository;
	private final MessageRepository messageRepository;
	private final ChatRepository chatRepository;
	private final SummaryRepository summaryRepository;
	private final SummaryDeadLetterRepository summaryDeadLetterRepository;
	private final LinkRepository linkRepository;
	private final ReportRepository reportRepository;
	private final MemberCommandService memberCommandService;
	private final MemberQueryService memberQueryService;

	@Transactional
	public void beginWithdrawal(Long memberId, String email) {
		Member member = memberQueryService.getUserForUpdate(email);
		validateMemberId(member, memberId);
		if (!member.isWithdrawing()) {
			member.beginWithdrawal();
		}
	}

	@Transactional
	public void deleteAll(Long memberId, String email) {
		Member member = memberQueryService.getUserForUpdate(email);
		validateMemberId(member, memberId);
		if (!member.isWithdrawing()) {
			throw new BusinessException(MemberErrorCode.WITHDRAWAL_IN_PROGRESS);
		}

		feedbackRepository.deleteAllByMemberId(memberId);
		messageRepository.deleteLinkMappingsByMemberId(memberId);
		messageRepository.deleteAllByMemberId(memberId);
		chatRepository.deleteAllByMemberId(memberId);
		summaryRepository.deleteAllByMemberId(memberId);
		summaryDeadLetterRepository.deleteAllByMemberId(memberId);
		linkRepository.deleteAllByMemberId(memberId);
		reportRepository.deleteAllByMemberId(memberId);
		memberCommandService.delete(member);
	}

	@Transactional
	public boolean claimWithdrawalAnalytics(Long memberId, String email) {
		Member member = memberQueryService.getUserForUpdate(email);
		validateMemberId(member, memberId);
		return member.claimWithdrawalAnalytics();
	}

	private void validateMemberId(Member member, Long memberId) {
		if (!member.getId().equals(memberId)) {
			throw new BusinessException(MemberErrorCode.MEMBER_ID_MISMATCH);
		}
	}
}
