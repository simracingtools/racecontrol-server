package de.bausdorf.simracing.racecontrol.web.model;

import de.bausdorf.simracing.racecontrol.orga.model.Penalty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PenaltySelectView {
	private String code;
	private String description;
	private boolean needsTimeParam;

	public static PenaltySelectView buildFromEntity(Penalty penalty) {
		return PenaltySelectView.builder()
				.code(penalty.getCode())
				.description(penalty.getCode() + " - " + penalty.getName())
				.needsTimeParam(penalty.getIRacingPenalty().isTimeParamNeeded())
				.build();
	}
}
