package com.sofa.linkiving.domain.link.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.error.LinkErrorCode;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.global.error.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LinkCommandService {

	private final LinkRepository linkRepository;
	private final LinkQueryService linkQueryService;

	public Link createLink(Member member, String url, String title, String memo,
		String imageUrl, String metadataJson, String tags, boolean isImportant) {
		if (linkRepository.existsByMemberAndUrlAndIsDeleteFalse(member, url)) {
			throw new BusinessException(LinkErrorCode.DUPLICATE_URL);
		}

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

		Link savedLink = linkRepository.save(link);
		log.info("Link created - id: {}, memberId: {}, url: {}",
			savedLink.getId(), member.getId(), url);

		return savedLink;
	}

	public Link updateLink(Long linkId, Member member, String title, String memo,
		String metadataJson, String tags, Boolean isImportant) {

		Link link = linkQueryService.findById(linkId, member);

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

		linkRepository.save(updatedLink);
		log.info("Link updated - id: {}, memberId: {}", linkId, member.getId());

		return updatedLink;
	}

	public void deleteLink(Long linkId, Member member) {
		Link link = linkQueryService.findById(linkId, member);
		link.markDeleted();
		log.info("Link soft deleted - id: {}, memberId: {}", linkId, member.getId());
	}
}
