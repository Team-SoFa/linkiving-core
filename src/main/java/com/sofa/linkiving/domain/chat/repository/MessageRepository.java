package com.sofa.linkiving.domain.chat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.member.entity.Member;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
	@Query("SELECT m FROM Message m JOIN m.chat c WHERE m.id = :id AND c.member = :member")
	Optional<Message> findByIdAndMember(@Param("id") Long id, @Param("member") Member member);
}
