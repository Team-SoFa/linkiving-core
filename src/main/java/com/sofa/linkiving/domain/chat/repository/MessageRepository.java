package com.sofa.linkiving.domain.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sofa.linkiving.domain.chat.dto.internal.RagHistoryLinkRow;
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

	@Query("""
		SELECT m FROM Message m LEFT JOIN FETCH m.feedback WHERE m.chat = :chat AND m.chat.member = :member
		AND m.chat.isDelete = false AND m.isDelete = false AND m.id < :beforeId ORDER BY m.id DESC
		""")
	List<Message> findRagHistory(@Param("chat") Chat chat, @Param("member") Member member,
		@Param("beforeId") Long beforeId, Pageable pageable);

	@Query("""
		SELECT new com.sofa.linkiving.domain.chat.dto.internal.RagHistoryLinkRow(m.id, l, s)
		FROM Message m JOIN m.links l
		LEFT JOIN Summary s ON s.link = l AND s.selected = true AND s.isDelete = false
		WHERE m.id IN :messageIds AND m.chat = :chat AND m.chat.member = :member
		AND m.chat.isDelete = false AND m.isDelete = false AND l.member = :member AND l.isDelete = false
		ORDER BY m.id DESC, l.id ASC
		""")
	List<RagHistoryLinkRow> findRagHistoryLinks(@Param("messageIds") List<Long> messageIds,
		@Param("chat") Chat chat, @Param("member") Member member);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = "DELETE FROM message_link WHERE message_id IN "
		+ "(SELECT m.id FROM messages m WHERE m.chat_id IN "
		+ "(SELECT c.id FROM chats c WHERE c.member_id = :memberId))", nativeQuery = true)
	void deleteLinkMappingsByMemberId(@Param("memberId") Long memberId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = "DELETE FROM messages WHERE chat_id IN "
		+ "(SELECT c.id FROM chats c WHERE c.member_id = :memberId)", nativeQuery = true)
	void deleteAllByMemberId(@Param("memberId") Long memberId);
}
