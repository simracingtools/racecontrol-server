package de.bausdorf.simracing.racecontrol.web.model.orga;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class LeagueApplicationView {
    private String displayName;
    private Long iracingId;
}
