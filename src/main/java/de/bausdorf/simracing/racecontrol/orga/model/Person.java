package de.bausdorf.simracing.racecontrol.orga.model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import de.bausdorf.simracing.racecontrol.orga.api.OrgaRoleType;
import lombok.Data;

@Data
@Entity
public class Person {
	@Id
	@GeneratedValue
	private long id;

	private String name;
	private String email;
	@ManyToOne(cascade = CascadeType.ALL)
	private OrganizationalUnit organisation;
	private OrgaRoleType role;
}
