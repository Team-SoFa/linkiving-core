package com.sofa.linkiving.domain.link.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sofa.linkiving.domain.link.entity.SummaryDeadLetter;
import com.sofa.linkiving.domain.link.enums.DeadLetterStatus;

public interface SummaryDeadLetterRepository extends JpaRepository<SummaryDeadLetter, Long> {

	Page<SummaryDeadLetter> findAllByStatus(DeadLetterStatus status, Pageable pageable);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM SummaryDeadLetter d WHERE d.memberId = :memberId")
	void deleteAllByMemberId(@Param("memberId") Long memberId);
}
