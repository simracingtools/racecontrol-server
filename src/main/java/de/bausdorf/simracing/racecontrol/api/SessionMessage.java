package de.bausdorf.simracing.racecontrol.api;

import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionMessage implements ClientData {
	private String trackName;
	private Duration sessionDuration;
	private String sessionType;
}
