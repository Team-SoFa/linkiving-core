package com.sofa.linkiving.domain.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.member.entity.Member;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
	List<Chat> findAllByMemberOrderByCreatedAtDesc(Member member);
}
