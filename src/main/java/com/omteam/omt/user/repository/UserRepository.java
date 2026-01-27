package com.omteam.omt.user.repository;

import com.omteam.omt.user.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findAllByDeletedAtIsNull();
}