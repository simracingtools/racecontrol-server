package de.bausdorf.simracing.racecontrol.model;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface DriverRepository extends CrudRepository<Driver, Integer> {

	Optional<Driver> findBySessionIdAndIracingId(String sessionId, long iRacingId);
}
