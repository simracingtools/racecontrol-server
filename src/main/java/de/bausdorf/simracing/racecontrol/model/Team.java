package de.bausdorf.simracing.racecontrol.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@Builder
@ToString
public class Team {
	private String name;
	private String id;
	private Driver currentDriver;
	long carNo;
	private Map<String, Driver> drivers;
	private List<DriverChange> driverChanges;
}
