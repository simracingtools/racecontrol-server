package de.bausdorf.simracing.racecontrol.model;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface TeamRepository extends CrudRepository<Team, Integer> {

	Optional<Team> findBySessionIdAndIracingId(String sessionId, long iRacingId);
}
