package com.omteam.omt.user.repository;

import com.omteam.omt.user.domain.UserOnboarding;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOnboardingRepository extends JpaRepository<UserOnboarding, Long> {

    Optional<UserOnboarding> findByUserId(Long userId);
}
