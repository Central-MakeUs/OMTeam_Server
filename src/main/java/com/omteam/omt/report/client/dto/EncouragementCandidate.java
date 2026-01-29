package com.omteam.omt.report.client.dto;

import com.omteam.omt.report.domain.EncouragementIntent;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EncouragementCandidate {

    private EncouragementIntent intent;
    private String title;
    private String message;
}
