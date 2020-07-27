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
public class DriverChange {
	private Driver changeFrom;
	private Driver changeTo;
	private Duration changeTime;
}
