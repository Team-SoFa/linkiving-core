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
		Link link = Link.builder()
			.member(member)
			.url(url)
			.title(title)
			.memo(memo)
			.imageUrl(imageUrl)
			.metadataJson(metadataJson)
			.tags(tags)
			.isImportant(isImportant)
			.build();

		return linkRepository.save(link);
	}

	public Link updateLink(Link link, String title, String memo,
		String metadataJson, String tags, Boolean isImportant) {
		Link updatedLink = Link.builder()
			.member(link.getMember())
			.url(link.getUrl())
			.title(title != null ? title : link.getTitle())
			.memo(memo != null ? memo : link.getMemo())
			.imageUrl(link.getImageUrl())
			.metadataJson(metadataJson != null ? metadataJson : link.getMetadataJson())
			.tags(tags != null ? tags : link.getTags())
			.isImportant(isImportant != null ? isImportant : link.isImportant())
			.build();

		return linkRepository.save(updatedLink);
	}

	public void deleteLink(Link link) {
		link.markDeleted();
	}
}
