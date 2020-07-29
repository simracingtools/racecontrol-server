package de.bausdorf.simracing.racecontrol.model;

import java.time.Duration;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
public class Stint {
	@Id
	@GeneratedValue
	long id;
	private Duration startTime;
	private Duration endTime;
	@ManyToOne(cascade = CascadeType.ALL)
	private Driver driver;
	@Column(nullable = false)
	private String sessionId;

	public Duration getStintDuration() {
		if( startTime != null && endTime != null ) {
			return endTime.minus(startTime);
		}
		return Duration.ZERO;
	}
}
