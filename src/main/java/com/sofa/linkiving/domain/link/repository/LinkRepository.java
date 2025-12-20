package com.sofa.linkiving.domain.link.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sofa.linkiving.domain.link.dto.internal.LinkDto;
import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.member.entity.Member;

public interface LinkRepository extends JpaRepository<Link, Long> {

	Optional<Link> findByIdAndMember(Long id, Member member);

	boolean existsByMemberAndUrlAndIsDeleteFalse(Member member, String url);

	@Query("""
		SELECT l.id
		FROM Link l
		WHERE l.member = :member AND l.url = :url AND l.isDelete = false
		""")
	Optional<Long> findIdByMemberAndUrlAndIsDeleteFalse(@Param("member") Member member, @Param("url") String url);

	@Query("""
		SELECT new com.sofa.linkiving.domain.link.dto.internal.LinkDto(l, s)
		FROM Link l
		LEFT JOIN Summary s ON s.link = l AND s.selected = true
		WHERE l.id = :id
		AND l.member = :member
		AND l.isDelete = false
		""")
	Optional<LinkDto> findByIdAndMemberWithSummaryAndIsDeleteFalse(
		@Param("id") Long id,
		@Param("member") Member member
	);

	@Query("""
		SELECT new com.sofa.linkiving.domain.link.dto.internal.LinkDto(l, s)
		FROM Link l
		LEFT JOIN Summary s ON s.link = l AND s.selected = true
		WHERE l.member = :member
		AND l.isDelete = false
		AND (:lastId IS NULL OR l.id < :lastId)
		ORDER BY l.id DESC
		""")
	List<LinkDto> findAllByMemberWithSummaryAndCursorAndIsDeleteFalse(
		@Param("member") Member member,
		@Param("lastId") Long lastId,
		Pageable pageable
	);
}
