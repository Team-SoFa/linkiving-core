package com.sofa.linkiving.domain.member.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.sofa.linkiving.domain.member.entity.Member;

public interface MemberRepository extends CrudRepository<Member, Long> {
	Optional<Member> findByEmail(String email);
}
