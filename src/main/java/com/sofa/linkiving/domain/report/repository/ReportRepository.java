package com.sofa.linkiving.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sofa.linkiving.domain.report.entity.Report;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM Report r WHERE r.member.id = :memberId")
	void deleteAllByMemberId(@Param("memberId") Long memberId);
}
