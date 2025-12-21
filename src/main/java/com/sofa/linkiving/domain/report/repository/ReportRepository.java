package com.sofa.linkiving.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sofa.linkiving.domain.report.entity.Report;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
}
