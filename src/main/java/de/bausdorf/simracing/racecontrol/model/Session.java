package de.bausdorf.simracing.racecontrol.model;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Session {
	@Id
	private String sessionId;
	private String trackName;
	private ZonedDateTime startTime;
	private Duration sessionDuration;
	@Column(nullable = false)
	private Timestamp lastUpdate;
}
