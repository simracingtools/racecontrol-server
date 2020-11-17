package de.bausdorf.simracing.racecontrol.orga.model;

import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class RuleViolation {
	@Id
	@GeneratedValue
	private long id;

	@ManyToOne
	private RuleViolationCategory category;
	private String identifier;

	private String violationReason;
	@ElementCollection(fetch = FetchType.EAGER)
	private Set<String> possiblePenaltyCodes;

	public String getCategoryDescription() {
		return category.getCategoryCode()
				+ " - "
				+ category.getCategoryName();
	}

	public String getDescription() {
		return identifier
				+ ") "
				+ violationReason;
	}
}
