package com.sofa.linkiving.domain.link.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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

	public Page<Link> findAllByMember(Member member, Pageable pageable) {
		return linkRepository.findByMemberAndIsDeleteFalse(member, pageable);
	}

	public boolean existsByUrl(Member member, String url) {
		return linkRepository.existsByMemberAndUrlAndIsDeleteFalse(member, url);
	}

	public Optional<Long> findIdByUrl(Member member, String url) {
		return linkRepository.findIdByMemberAndUrlAndIsDeleteFalse(member, url);
	}
}
