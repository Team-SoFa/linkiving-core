package com.sofa.linkiving.domain.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Message;
import com.sofa.linkiving.domain.member.entity.Member;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
	@Query("SELECT m FROM Message m JOIN m.chat c WHERE m.id = :id AND c.member = :member")
	Optional<Message> findByIdAndMember(@Param("id") Long id, @Param("member") Member member);

	@Modifying(clearAutomatically = true)
	@Query("DELETE FROM Message m WHERE m.chat = :chat")
	void deleteAllByChat(Chat chat);

	@Query("""
		SELECT m FROM Message m
		LEFT JOIN FETCH m.feedback
		WHERE m.chat = :chat
		AND (:lastId IS NULL OR m.id < :lastId)
		ORDER BY m.id DESC
		""")
	List<Message> findAllByChatAndCursor(
		@Param("chat") Chat chat,
		@Param("lastId") Long lastId,
		Pageable pageable
	);

	List<Message> findAllByChat(Chat chat);

	List<Message> findTop7ByChatAndIdLessThanOrderByIdDesc(Chat chat, Long id);
}
