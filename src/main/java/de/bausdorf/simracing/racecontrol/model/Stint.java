package de.bausdorf.simracing.racecontrol.model;

import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@Builder
@ToString
public class Stint {
	private Duration startTime;
	private Duration endTime;

	public Duration getStintDuration() {
		if( startTime != null && endTime != null ) {
			return endTime.minus(startTime);
		}
		return Duration.ZERO;
	}
}
