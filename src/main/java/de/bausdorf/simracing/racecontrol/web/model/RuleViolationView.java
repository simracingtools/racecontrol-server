package de.bausdorf.simracing.racecontrol.web.model;

import java.util.HashSet;
import java.util.Set;

import org.springframework.lang.Nullable;

import de.bausdorf.simracing.racecontrol.orga.model.RuleViolation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RuleViolationView {
	private long id;

	private String categoryCode;
	private String identifier;
	private String violationReason;
	private Set<String> possiblePenaltyCodes;

	public String getPenaltyCodesAsString() {
		StringBuilder penaltyCodes = new StringBuilder();
		possiblePenaltyCodes.stream().forEach(s -> {
			penaltyCodes.append(s);
			penaltyCodes.append(' ');
		});
		return penaltyCodes.toString().trim();
	}

	public static RuleViolationView buildEmpty() {
		return RuleViolationView.builder()
				.categoryCode("")
				.id(0)
				.identifier("")
				.violationReason("")
				.possiblePenaltyCodes(new HashSet<>())
				.build();
	}

	public static RuleViolationView buildFromEntity(@Nullable RuleViolation violation) {
		if(violation == null) {
			return buildEmpty();
		}
		return RuleViolationView.builder()
				.id(violation.getId())
				.categoryCode(violation.getCategory().getCategoryCode())
				.identifier(violation.getIdentifier())
				.violationReason(violation.getViolationReason())
				.possiblePenaltyCodes(violation.getPossiblePenaltyCodes())
				.build();
	}
}
