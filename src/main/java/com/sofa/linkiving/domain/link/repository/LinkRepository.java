package com.sofa.linkiving.domain.link.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sofa.linkiving.domain.link.entity.Link;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {

	// TODO: 추후 필요한 쿼리 메서드 추가
}
