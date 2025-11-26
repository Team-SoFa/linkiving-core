package com.sofa.linkiving.domain.link.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class LinkCommandService {

	private final LinkRepository linkRepository;

	public Link saveLink(Member member, String url, String title, String memo,
		String imageUrl, String metadataJson, String tags, boolean isImportant) {
		Link link = Link.create(member, url, title, memo, imageUrl, metadataJson, tags, isImportant);
		return linkRepository.save(link);
	}

	public Link updateLink(Link link, String title, String memo,
		String metadataJson, String tags, Boolean isImportant) {
		link.updateDetails(title, memo, metadataJson, tags, isImportant);
		return link;
	}

	public void deleteLink(Link link) {
		link.markDeleted();
	}
}
