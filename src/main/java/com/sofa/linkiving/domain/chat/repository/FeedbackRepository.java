package com.sofa.linkiving.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sofa.linkiving.domain.chat.entity.Chat;
import com.sofa.linkiving.domain.chat.entity.Feedback;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
	@Modifying(clearAutomatically = true)
	@Query("DELETE FROM Feedback f WHERE f.message.id IN (SELECT m.id FROM Message m WHERE m.chat = :chat)")
	void deleteAllByChat(@Param("chat") Chat chat);
}
