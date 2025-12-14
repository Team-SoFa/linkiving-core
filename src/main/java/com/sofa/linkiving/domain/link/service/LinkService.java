package com.sofa.linkiving.domain.link.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.link.dto.response.LinkDuplicateCheckRes;
import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LinkService {

	private final LinkCommandService linkCommandService;
	private final LinkQueryService linkQueryService;

	public LinkRes createLink(Member member, String url, String title, String memo,
		String imageUrl, String metadataJson, String tags, boolean isImportant) {
		if (linkQueryService.existsByUrl(member, url)) {
			throw new BusinessException(LinkErrorCode.DUPLICATE_URL);
		}

		Link link = linkCommandService.saveLink(member, url, title, memo, imageUrl, metadataJson, tags, isImportant);
		log.info("Link created - id: {}, memberId: {}, url: {}", link.getId(), member.getId(), url);

		return LinkRes.from(link);
	}

	public LinkRes updateLink(Long linkId, Member member, String title, String memo,
		String metadataJson, String tags, Boolean isImportant) {
		Link link = linkQueryService.findById(linkId, member);
		Link updatedLink = linkCommandService.updateLink(link, title, memo, metadataJson, tags, isImportant);

		log.info("Link updated - id: {}, memberId: {}", linkId, member.getId());

		return LinkRes.from(updatedLink);
	}

	public LinkRes updateTitle(Long linkId, Member member, String title) {
		Link link = linkQueryService.findById(linkId, member);
		Link updatedLink = linkCommandService.updateLink(
			link,
			title,
			link.getMemo(),
			link.getMetadataJson(),
			link.getTags(),
			link.isImportant()
		);

		log.info("Link title updated - id: {}, memberId: {}", linkId, member.getId());

		return LinkRes.from(updatedLink);
	}

	public LinkRes updateMemo(Long linkId, Member member, String memo) {
		Link link = linkQueryService.findById(linkId, member);
		Link updatedLink = linkCommandService.updateLink(
			link,
			link.getTitle(),
			memo,
			link.getMetadataJson(),
			link.getTags(),
			link.isImportant()
		);

		log.info("Link memo updated - id: {}, memberId: {}", linkId, member.getId());

		return LinkRes.from(updatedLink);
	}

	public void deleteLink(Long linkId, Member member) {
		Link link = linkQueryService.findById(linkId, member);
		linkCommandService.deleteLink(link);

		log.info("Link soft deleted - id: {}, memberId: {}", linkId, member.getId());
	}

	@Transactional(readOnly = true)
	public LinkRes getLink(Long linkId, Member member) {
		Link link = linkQueryService.findById(linkId, member);
		return LinkRes.from(link);
	}

	@Transactional(readOnly = true)
	public Page<LinkRes> getLinkList(Member member, Pageable pageable) {
		Page<Link> links = linkQueryService.findAllByMember(member, pageable);
		return links.map(LinkRes::from);
	}

	@Transactional(readOnly = true)
	public LinkDuplicateCheckRes checkDuplicate(Member member, String url) {
		return linkQueryService.findIdByUrl(member, url)
			.map(LinkDuplicateCheckRes::exists)
			.orElse(LinkDuplicateCheckRes.notExists());
	}
}
