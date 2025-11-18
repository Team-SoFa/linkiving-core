package com.sofa.linkiving.domain.link.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.member.entity.Member;

public interface LinkRepository extends JpaRepository<Link, Long> {

	Optional<Link> findByIdAndMember(Long id, Member member);

	Page<Link> findByMemberAndIsDeleteFalse(Member member, Pageable pageable);

	boolean existsByMemberAndUrlAndIsDeleteFalse(Member member, String url);
}
