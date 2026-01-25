package com.omteam.omt.character.repository;

import com.omteam.omt.character.domain.DailyEncouragementSet;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyEncouragementSetRepository extends JpaRepository<DailyEncouragementSet, Long> {

    Optional<DailyEncouragementSet> findByUserUserIdAndTargetDate(Long userId, LocalDate targetDate);

    boolean existsByUserUserIdAndTargetDate(Long userId, LocalDate targetDate);
}
