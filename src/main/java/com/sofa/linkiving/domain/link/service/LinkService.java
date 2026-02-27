package com.sofa.linkiving.domain.link.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.dto.internal.LinksDto;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkService {

	private final LinkCommandService linkCommandService;
	private final LinkQueryService linkQueryService;

	public Link createLink(Member member, String url, String title, String memo, String imageUrl) {
		if (linkQueryService.existsByUrl(member, url)) {
			throw new BusinessException(LinkErrorCode.DUPLICATE_URL);
		}

		Link link = linkCommandService.saveLink(member, url, title, memo, imageUrl);
		log.info("Link created - id: {}, memberId: {}, url: {}", link.getId(), member.getId(), url);

		return link;
	}

	public Link updateLink(Long linkId, Member member, String title, String memo) {
		Link link = linkQueryService.findById(linkId, member);
		Link updatedLink = linkCommandService.updateLink(link, title, memo);

		log.info("Link updated - id: {}, memberId: {}", linkId, member.getId());

		return updatedLink;
	}

	public Link updateTitle(Long linkId, Member member, String title) {
		Link link = linkQueryService.findById(linkId, member);
		Link updatedLink = linkCommandService.updateLink(link, title, link.getMemo());

		log.info("Link title updated - id: {}, memberId: {}", linkId, member.getId());

		return updatedLink;
	}

	public Link updateMemo(Long linkId, Member member, String memo) {
		Link link = linkQueryService.findById(linkId, member);
		Link updatedLink = linkCommandService.updateLink(link, link.getTitle(), memo);

		log.info("Link memo updated - id: {}, memberId: {}", linkId, member.getId());

		return updatedLink;
	}

	public void deleteLink(Long linkId, Member member) {
		Link link = linkQueryService.findById(linkId, member);
		linkCommandService.deleteLink(link);

		log.info("Link soft deleted - id: {}, memberId: {}", linkId, member.getId());
	}

	public Link getLink(Long linkId) {
		return linkQueryService.findById(linkId);
	}

	public Link getLink(Long linkId, Member member) {
		return linkQueryService.findById(linkId, member);
	}

	public LinkDto getLinkWithSummary(Long linkId, Member member) {
		return linkQueryService.findByIdWithSummary(linkId, member);
	}

	public LinksDto getLinksWithSummary(Member member, Long lastId, int size) {
		return linkQueryService.findAllByMemberWithSummaryAndCursor(member, lastId, size);
	}

	public Optional<Long> findLinkIdByUrl(Member member, String url) {
		return linkQueryService.findIdByUrl(member, url);
	}
}
