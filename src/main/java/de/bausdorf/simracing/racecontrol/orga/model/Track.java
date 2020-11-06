package de.bausdorf.simracing.racecontrol.orga.model;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Track {
	@Id
	private long trackId;

	private String screenName;
	private String shortName;
}
