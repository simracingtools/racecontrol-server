package de.bausdorf.simracing.racecontrol.orga.model;

import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import de.bausdorf.simracing.racecontrol.orga.api.TrackEventType;
import lombok.Data;

@Data
@Entity
public class TrackEvent {
	@Id
	@GeneratedValue
	private long id;

	private String title;
	private ZonedDateTime dateTime;
	private TrackEventType type;
	@ManyToOne
	RuleSet ruleSet;
	@ManyToOne
	OrganizationalUnit organizingUnit;
	@ManyToOne
	private Track track;
	@OneToMany(mappedBy = "id", fetch = FetchType.EAGER)
	private List<CarClass> carClasses;
}
