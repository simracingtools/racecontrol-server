package de.bausdorf.simracing.racecontrol.orga.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.Data;

@Data
@Entity
public class OrganizationalUnit {
	@Id
	@GeneratedValue
	private long id;

	private String name;
	@OneToMany(mappedBy = "organization", fetch = FetchType.EAGER)
	private List<Person> staff;
}
