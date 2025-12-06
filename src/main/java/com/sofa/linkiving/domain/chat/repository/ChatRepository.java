package com.sofa.linkiving.domain.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sofa.linkiving.domain.chat.entity.Chat;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
}
