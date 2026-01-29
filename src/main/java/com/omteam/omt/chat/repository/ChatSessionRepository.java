package com.omteam.omt.chat.repository;

import com.omteam.omt.chat.domain.ChatSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    /**
     * 사용자의 활성 세션 조회
     */
    Optional<ChatSession> findByUserUserIdAndIsActiveTrue(Long userId);

    /**
     * 사용자의 모든 세션 존재 여부 확인
     */
    boolean existsByUserUserId(Long userId);
}
