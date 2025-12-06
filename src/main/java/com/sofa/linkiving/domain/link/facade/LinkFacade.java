package com.sofa.linkiving.domain.link.facade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sofa.linkiving.domain.link.dto.response.LinkRes;
import com.sofa.linkiving.domain.link.service.LinkService;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LinkFacade {

	private final LinkService linkService;

	public LinkRes createLink(Member member, String url, String title, String memo,
		String imageUrl, String metadataJson, String tags, boolean isImportant) {
		return linkService.createLink(member, url, title, memo, imageUrl, metadataJson, tags, isImportant);
	}

	public LinkRes updateLink(Long linkId, Member member, String title, String memo,
		String metadataJson, String tags, Boolean isImportant) {
		return linkService.updateLink(linkId, member, title, memo, metadataJson, tags, isImportant);
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

	public LinkRes getLink(Long linkId, Member member) {
		return linkService.getLink(linkId, member);
	}

	public Page<LinkRes> getLinkList(Member member, Pageable pageable) {
		return linkService.getLinkList(member, pageable);
	}

	public boolean checkDuplicate(Member member, String url) {
		return linkService.checkDuplicate(member, url);
	}
}
