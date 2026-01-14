package com.omteam.omt.user.repository;

import com.omteam.omt.user.domain.UserCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {
}
