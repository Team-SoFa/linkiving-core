package com.sofa.linkiving.domain.link.facade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.link.dto.response.LinkDuplicateCheckRes;
import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.link.service.LinkService;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class LinkFacade {

	private final LinkService linkService;

	public LinkRes createLink(Member member, String url, String title, String memo, String imageUrl) {
		return linkService.createLink(member, url, title, memo, imageUrl);
	}

	public LinkRes updateLink(Long linkId, Member member, String title, String memo) {
		return linkService.updateLink(linkId, member, title, memo);
	}

	public LinkRes updateTitle(Long linkId, Member member, String title) {
		return linkService.updateTitle(linkId, member, title);
	}

	public LinkRes updateMemo(Long linkId, Member member, String memo) {
		return linkService.updateMemo(linkId, member, memo);
	}

	public void deleteLink(Long linkId, Member member) {
		linkService.deleteLink(linkId, member);
	}

	@Transactional(readOnly = true)
	public LinkRes getLink(Long linkId, Member member) {
		return linkService.getLink(linkId, member);
	}

	@Transactional(readOnly = true)
	public Page<LinkRes> getLinkList(Member member, Pageable pageable) {
		return linkService.getLinkList(member, pageable);
	}

	@Transactional(readOnly = true)
	public LinkDuplicateCheckRes checkDuplicate(Member member, String url) {
		return linkService.checkDuplicate(member, url);
	}
}
