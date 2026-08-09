package com.sofa.linkiving.domain.member.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sofa.linkiving.domain.member.entity.Member;
import com.sofa.linkiving.domain.member.enums.MemberStatus;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
	boolean existsMemberByEmail(String email);

	Optional<Member> findByEmail(String email);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		DELETE FROM Member m
		WHERE m.status = :status
			AND m.termsAgreedAt IS NULL
			AND m.privacyAgreedAt IS NULL
			AND m.createdAt < :createdAt
		""")
	long deleteByStatusAndTermsAgreedAtIsNullAndPrivacyAgreedAtIsNullAndCreatedAtBefore(
		@Param("status") MemberStatus status,
		@Param("createdAt") LocalDateTime createdAt
	);
}
