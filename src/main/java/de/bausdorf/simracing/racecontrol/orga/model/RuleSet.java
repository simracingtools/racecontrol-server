package de.bausdorf.simracing.racecontrol.orga.model;

import java.time.Duration;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class RuleSet {
	@Id
	@GeneratedValue
	private long id;

	private Duration maxDrivingTime;
	private Duration maxDrivingTimeRequiresRest;
	private Duration minRestTime;
	private int fairShareFactor;
	private boolean fairShareByLap;
}
