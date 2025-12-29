package com.sofa.linkiving.domain.link.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sofa.linkiving.domain.link.entity.Summary;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {

	Optional<Summary> findByLinkIdAndSelectedTrue(Long linkId);

	boolean existsByLinkIdAndSelectedTrue(Long linkId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Summary s set s.selected = false where s.link.id = :linkId and s.selected = true")
	int clearSelectedByLinkId(@Param("linkId") Long linkId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Summary s set s.selected = true where s.id = :summaryId and s.link.id = :linkId")
	int selectByIdAndLinkId(@Param("summaryId") Long summaryId, @Param("linkId") Long linkId);
}
