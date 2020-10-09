package de.bausdorf.simracing.racecontrol.web.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SessionSelectView {
	private String selectedSessionId;
	List<SessionOptionView> sessions;
}
