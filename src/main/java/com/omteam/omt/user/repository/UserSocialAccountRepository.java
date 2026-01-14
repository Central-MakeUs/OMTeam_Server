package com.omteam.omt.user.repository;

import com.omteam.omt.user.domain.UserSocialAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {
    <T> Optional<T> findByProviderUserId(String providerUserId);
}
