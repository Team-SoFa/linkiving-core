package com.sofa.linkiving.domain.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.member.entity.Member;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
	@Query("""
		SELECT c
		FROM Chat c
		JOIN Message m ON m.chat = c
		WHERE c.member = :member
		GROUP BY c
		ORDER BY MAX(m.createdAt) DESC
		""")
	List<Chat> findAllByMemberOrderByLastMessageDesc(@Param("member") Member member);

	Optional<Chat> findByIdAndMember(Long id, Member member);
}
