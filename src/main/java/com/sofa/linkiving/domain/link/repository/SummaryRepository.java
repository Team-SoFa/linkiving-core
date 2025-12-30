package com.sofa.linkiving.domain.link.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sofa.linkiving.domain.link.entity.Link;
import com.sofa.linkiving.domain.link.entity.Summary;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {
	@Query("SELECT s FROM Summary s WHERE s.link IN :links AND s.selected = true")
	List<Summary> findAllByLinkInAndSelectedTrue(@Param("links") List<Link> links);
}
