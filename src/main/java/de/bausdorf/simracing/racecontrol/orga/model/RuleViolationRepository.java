package de.bausdorf.simracing.racecontrol.orga.model;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface RuleViolationRepository extends CrudRepository<RuleViolation, Long> {

	List<RuleViolation> findAllByCategory(RuleViolationCategory category);

	Optional<RuleViolation> findRuleViolationByCategoryAndIdentifier(RuleViolationCategory category, String identifier);
}
