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
public class ClientMessage {

	private ClientMessageType type;
	private String version;
	private long eventNo;
	private String sessionId;
	private Integer teamId;
	private Integer driverId;
	private long iRating;
	private String driverName;
	private String teamName;
	private EventType eventType;
	private long carNo;
	private long lap;
	private Duration sessionTime;
}
