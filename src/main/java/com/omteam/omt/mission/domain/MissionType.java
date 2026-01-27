package com.omteam.omt.mission.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MissionType {
    EXERCISE("운동"),
    DIET("식단");

    private final String displayName;
}
