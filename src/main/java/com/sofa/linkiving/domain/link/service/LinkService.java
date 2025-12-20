package com.sofa.linkiving.domain.link.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.link.dto.response.LinkDuplicateCheckRes;
import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.event.LinkCreatedEvent;
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
	private final ApplicationEventPublisher eventPublisher;

	public LinkRes createLink(Member member, String url, String title, String memo, String imageUrl) {
		if (linkQueryService.existsByUrl(member, url)) {
			throw new BusinessException(LinkErrorCode.DUPLICATE_URL);
		}

		Link link = linkCommandService.saveLink(member, url, title, memo, imageUrl);
		log.info("Link created - id: {}, memberId: {}, url: {}", link.getId(), member.getId(), url);

		// 트랜잭션 커밋 후 요약 대기 큐에 추가되도록 이벤트 발행
		eventPublisher.publishEvent(new LinkCreatedEvent(link.getId()));

		return LinkRes.from(link);
	}

	public LinkRes updateLink(Long linkId, Member member, String title, String memo) {
		Link link = linkQueryService.findById(linkId, member);
		Link updatedLink = linkCommandService.updateLink(link, title, memo);

		log.info("Link updated - id: {}, memberId: {}", linkId, member.getId());

		return LinkRes.from(updatedLink);
	}

	public LinkRes updateTitle(Long linkId, Member member, String title) {
		Link link = linkQueryService.findById(linkId, member);
		Link updatedLink = linkCommandService.updateLink(link, title, link.getMemo());

		log.info("Link title updated - id: {}, memberId: {}", linkId, member.getId());

		return LinkRes.from(updatedLink);
	}

	public LinkRes updateMemo(Long linkId, Member member, String memo) {
		Link link = linkQueryService.findById(linkId, member);
		Link updatedLink = linkCommandService.updateLink(link, link.getTitle(), memo);

		log.info("Link memo updated - id: {}, memberId: {}", linkId, member.getId());

		return LinkRes.from(updatedLink);
	}

	public void deleteLink(Long linkId, Member member) {
		Link link = linkQueryService.findById(linkId, member);
		linkCommandService.deleteLink(link);

		log.info("Link soft deleted - id: {}, memberId: {}", linkId, member.getId());
	}

	public LinkRes getLink(Long linkId, Member member) {
		Link link = linkQueryService.findById(linkId, member);
		return LinkRes.from(link);
	}

	public Page<LinkRes> getLinkList(Member member, Pageable pageable) {
		Page<Link> links = linkQueryService.findAllByMember(member, pageable);
		return links.map(LinkRes::from);
	}

	public LinkDuplicateCheckRes checkDuplicate(Member member, String url) {
		return linkQueryService.findIdByUrl(member, url)
			.map(LinkDuplicateCheckRes::exists)
			.orElse(LinkDuplicateCheckRes.notExists());
	}
}
