package de.bausdorf.simracing.racecontrol.web.model.orga;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisteredCarEditView {
    private long eventId;
    private long teamId;
    private long carId;
    private boolean useWildcard;
}
