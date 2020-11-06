package de.bausdorf.simracing.racecontrol.orga.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Data;

@Data
@Entity
public class BalancedCar {
	@Id
	@GeneratedValue
	private long id;

	@ManyToOne
	private Car car;
	private int fuelPercent;
	private int weightPenalty;
	private int enginePowerPercent;
}
