package de.bausdorf.simracing.racecontrol.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
public class Team extends BaseEntity {
	private String name;
	@ManyToOne
	private Driver currentDriver;
	long carNo;
	@OneToMany(mappedBy = "team", fetch = FetchType.EAGER)
	private List<Driver> drivers;

	public Team() {
		drivers = new ArrayList<>();
	}

	@Builder
	public Team(String sessionId, long teamId, String name, Driver currentDriver, long carNo, List<Driver> drivers) {
		super(sessionId, teamId);
		this.name = name;
		this.currentDriver = currentDriver;
		this.carNo = carNo;
		this.drivers = drivers;
	}

	public long getTeamId() {
		return super.getIracingId();
	}

	public boolean containsDriver(long driverId) {
		return drivers.stream()
				.filter(s -> s.getIracingId() == driverId)
				.collect(Collectors.toSet()).stream().findFirst().isPresent();
	}
}
