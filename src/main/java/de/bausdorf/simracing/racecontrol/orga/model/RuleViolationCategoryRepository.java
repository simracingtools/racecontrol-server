package de.bausdorf.simracing.racecontrol.orga.model;

import java.util.stream.Stream;

import org.springframework.data.repository.CrudRepository;

public interface RuleViolationCategoryRepository extends CrudRepository<RuleViolationCategory, String> {

	Stream<RuleViolationCategory> findAllByCategoryCodeContainingOrderByCategoryCodeAsc(String code);
}
