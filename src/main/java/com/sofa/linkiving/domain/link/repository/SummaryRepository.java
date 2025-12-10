package com.sofa.linkiving.domain.link.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sofa.linkiving.domain.link.entity.Summary;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {
}
