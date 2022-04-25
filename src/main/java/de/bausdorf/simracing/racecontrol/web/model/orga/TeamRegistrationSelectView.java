package de.bausdorf.simracing.racecontrol.web.model.orga;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRegistrationSelectView {
    private Long eventId;
    private String teamId;
}
