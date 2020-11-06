package de.bausdorf.simracing.racecontrol.orga.model;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Car {
	@Id
	private long carId;

	private String name;
	private long maxFuel;
}
