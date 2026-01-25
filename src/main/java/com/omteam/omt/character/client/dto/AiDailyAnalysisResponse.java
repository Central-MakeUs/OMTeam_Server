package com.omteam.omt.character.client.dto;

import com.omteam.omt.character.domain.EncouragementIntent;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiDailyAnalysisResponse {

    private String feedbackText;
    private List<EncouragementCandidate> encouragementCandidates;

    public EncouragementCandidate getEncouragementOf(EncouragementIntent intent) {
        return encouragementCandidates.stream()
                .filter(c -> c.getIntent() == intent)
                .findFirst()
                .orElse(null);
    }
}
