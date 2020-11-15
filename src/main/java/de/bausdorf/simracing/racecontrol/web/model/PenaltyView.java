package de.bausdorf.simracing.racecontrol.web.model;

import org.springframework.lang.Nullable;

import de.bausdorf.simracing.racecontrol.api.IRacingPenalty;
import de.bausdorf.simracing.racecontrol.orga.model.Penalty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PenaltyView {
	private String code;
	private String name;
	private String iRacingPenalty;

	public void updateEntity(Penalty entity) {
		entity.setCode(code);
		entity.setName(name);
		entity.setIRacingPenalty(IRacingPenalty.valueOf(iRacingPenalty));
	}

	public static PenaltyView buildFromEntity(@Nullable Penalty penalty) {
		if(penalty == null) {
			return buildEmpty();
		}
		return PenaltyView.builder()
				.code(penalty.getCode())
				.name(penalty.getName())
				.iRacingPenalty(penalty.getIRacingPenalty().name())
				.build();
	}

	public static PenaltyView buildEmpty() {
		return PenaltyView.builder()
				.code("")
				.name("")
				.iRacingPenalty("")
				.build();
	}
}
