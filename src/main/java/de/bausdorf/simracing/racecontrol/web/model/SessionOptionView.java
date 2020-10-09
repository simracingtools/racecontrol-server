package de.bausdorf.simracing.racecontrol.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SessionOptionView {
	private String sessionId;
	private String displayName;
}
