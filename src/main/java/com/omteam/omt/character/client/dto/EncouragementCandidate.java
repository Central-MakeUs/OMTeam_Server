package com.omteam.omt.character.client.dto;

import com.omteam.omt.character.domain.EncouragementIntent;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EncouragementCandidate {

    private EncouragementIntent intent;
    private String title;
    private String message;
}