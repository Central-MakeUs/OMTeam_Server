package com.omteam.omt.chat.repository;

import com.omteam.omt.chat.domain.ChatMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 세션의 모든 메시지 조회 (시간순 정렬, AI 요청용)
     */
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    /**
     * 사용자의 최신 메시지 조회 (커서 기반 페이지네이션 - 첫 페이지)
     */
    @Query("SELECT m FROM ChatMessage m " +
            "JOIN m.session s " +
            "WHERE s.user.userId = :userId " +
            "ORDER BY m.id DESC")
    List<ChatMessage> findLatestMessagesByUserId(
            @Param("userId") Long userId,
            Pageable pageable);

    /**
     * 사용자의 메시지 조회 (커서 기반 페이지네이션 - 이후 페이지)
     */
    @Query("SELECT m FROM ChatMessage m " +
            "JOIN m.session s " +
            "WHERE s.user.userId = :userId " +
            "AND m.id < :cursor " +
            "ORDER BY m.id DESC")
    List<ChatMessage> findMessagesByUserIdAndCursor(
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Pageable pageable);

    /**
     * 세션의 메시지 수 조회
     */
    long countBySessionId(Long sessionId);
}
