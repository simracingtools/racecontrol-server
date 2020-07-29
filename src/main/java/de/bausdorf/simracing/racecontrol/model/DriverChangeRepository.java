package de.bausdorf.simracing.racecontrol.model;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface DriverChangeRepository extends CrudRepository<DriverChange, Integer> {

	List<DriverChange> findBySessionIdAndTeam(String sessionId, Team team);
}
