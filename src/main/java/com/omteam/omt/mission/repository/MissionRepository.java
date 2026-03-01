package com.omteam.omt.mission.repository;

import com.omteam.omt.mission.domain.Mission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    @Query(value = "SELECT * FROM mission WHERE type = :type ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Mission> findRandomByType(@Param("type") String type, @Param("count") int count);

    @Query(value = "SELECT * FROM mission ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Mission> findRandom(@Param("count") int count);
}
