package de.bausdorf.simracing.racecontrol.model;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface SessionRepository extends CrudRepository<Session, Integer> {

	Optional<Session> findBySessionId(String sessionId);
}
