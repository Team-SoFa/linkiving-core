package com.sofa.linkiving.domain.link.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sofa.linkiving.domain.link.dto.request.LinkSyncUpdateReq;
import com.sofa.linkiving.domain.link.repository.LinkRepository;
import com.sofa.linkiving.domain.member.enums.MemberStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
public class LinkSyncSnapshotService {

	private final LinkRepository linkRepository;

	public Optional<LinkSyncUpdateReq> findCurrent(Long linkId) {
		// AFTER_COMMIT가 CallerRunsPolicy로 실행돼도 완료된 트랜잭션의 영속성 컨텍스트를 재사용하지 않는다.
		return linkRepository.findSyncSnapshot(linkId, MemberStatus.ACTIVE)
			.map(snapshot -> LinkSyncUpdateReq.of(snapshot.link(), snapshot.summary()));
	}

	public boolean isSyncTarget(Long linkId) {
		return linkRepository.existsSyncTarget(linkId, MemberStatus.ACTIVE);
	}
}
