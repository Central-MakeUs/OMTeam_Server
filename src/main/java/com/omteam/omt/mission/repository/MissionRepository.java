package com.omteam.omt.mission.repository;

import com.omteam.omt.mission.domain.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionRepository extends JpaRepository<Mission, Long> {
}
