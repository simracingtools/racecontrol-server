package de.bausdorf.simracing.racecontrol.orga.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import de.bausdorf.simracing.racecontrol.orga.api.RegistrationStateType;
import lombok.Data;

@Data
@Entity
public class Registration {
	@Id
	@GeneratedValue
	private long id;

	private String name;
	private String emailContact;
	private String discordHandle;
	@ManyToOne
	private CarClass carClass;
	@ManyToOne
	private Car car;
	private RegistrationStateType state;
}
