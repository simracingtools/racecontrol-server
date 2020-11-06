package de.bausdorf.simracing.racecontrol.orga.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import lombok.Data;

@Data
@Entity
public class EventSeries {
	@Id
	@GeneratedValue
	private long id;

	private String title;
	private String logoUrl;
	private String discordLink;
	private String discordInvite;
	@ManyToOne
	RuleSet ruleSet;
	@ManyToOne
	OrganizationalUnit organizingUnit;
	@OneToMany(mappedBy = "id", fetch = FetchType.EAGER)
	private List<CarClass> carClassPreset;
	@OneToMany(mappedBy = "id", fetch = FetchType.EAGER)
	private List<TrackEvent> trackEvents;
}
