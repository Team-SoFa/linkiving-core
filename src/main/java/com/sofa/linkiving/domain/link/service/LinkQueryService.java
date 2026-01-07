package com.sofa.linkiving.domain.link.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.dto.internal.LinksDto;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkQueryService {

	private final LinkRepository linkRepository;

	public Link findById(Long linkId, Member member) {
		return linkRepository.findByIdAndMember(linkId, member)
			.filter(link -> !link.isDeleted())
			.orElseThrow(() -> new BusinessException(LinkErrorCode.LINK_NOT_FOUND));
	}

	public LinkDto findByIdWithSummary(Long linkId, Member member) {
		return linkRepository.findByIdAndMemberWithSummaryAndIsDeleteFalse(linkId, member)
			.orElseThrow(() -> new BusinessException(LinkErrorCode.LINK_NOT_FOUND));
	}

	public List<LinkDto> findAllByIdInWithSummary(List<Long> linkIds, Member member) {
		return linkRepository.findAllByMemberAndIdInWithSummaryAndIsDeleteFalse(linkIds, member);
	}

	public LinksDto findAllByMemberWithSummaryAndCursor(Member member, Long lastId, int size) {
		PageRequest pageRequest = PageRequest.of(0, size + 1);
		List<LinkDto> linkDtos = linkRepository.findAllByMemberWithSummaryAndCursorAndIsDeleteFalse(member, lastId,
			pageRequest);

		boolean hasNext = false;
		if (linkDtos.size() > size) {
			hasNext = true;
			linkDtos.remove(size);
		}

		return new LinksDto(linkDtos, hasNext);
	}

	public boolean existsByUrl(Member member, String url) {
		return linkRepository.existsByMemberAndUrlAndIsDeleteFalse(member, url);
	}

	public Optional<Long> findIdByUrl(Member member, String url) {
		return linkRepository.findIdByMemberAndUrlAndIsDeleteFalse(member, url);
	}
}
