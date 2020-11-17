package de.bausdorf.simracing.racecontrol.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RuleViolationSelectView {
	private long id;
	private String description;
}
