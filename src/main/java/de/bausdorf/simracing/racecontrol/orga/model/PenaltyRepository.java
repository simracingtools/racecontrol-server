package de.bausdorf.simracing.racecontrol.orga.model;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;

public interface PenaltyRepository extends CrudRepository<Penalty, String> {

	Set<Penalty> findPenaltiesByCodeIn(Set<String> codes);
}
