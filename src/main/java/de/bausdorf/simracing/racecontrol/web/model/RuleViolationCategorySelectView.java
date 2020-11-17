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
public class RuleViolationCategorySelectView {
	private String code;
	private String description;
	private List<RuleViolationView> violations;
}
