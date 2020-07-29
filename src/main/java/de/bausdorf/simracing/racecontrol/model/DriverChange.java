package de.bausdorf.simracing.racecontrol.model;

import java.time.Duration;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
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
public class DriverChange {
	@Id
	@GeneratedValue
	long id;
	@ManyToOne
	private Driver changeFrom;
	@ManyToOne
	private Driver changeTo;
	@ManyToOne
	private Team team;
	private Duration changeTime;
	@Column(nullable = false)
	private String sessionId;
}
