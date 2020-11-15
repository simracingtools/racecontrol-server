package de.bausdorf.simracing.racecontrol.orga.model;

import java.util.Set;
import java.util.stream.Stream;

import org.springframework.data.repository.CrudRepository;

public interface PenaltyRepository extends CrudRepository<Penalty, String> {

	Set<Penalty> findPenaltiesByCodeIn(Set<String> codes);
	Stream<Penalty> findAllByCodeContainingOrderByCodeAsc(String codePart);
}
