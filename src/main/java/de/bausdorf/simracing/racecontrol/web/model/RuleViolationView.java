package de.bausdorf.simracing.racecontrol.web.model;

/*-
 * #%L
 * racecontrol-server
 * %%
 * Copyright (C) 2020 bausdorf engineering
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.util.HashSet;
import java.util.Set;

import org.springframework.lang.Nullable;

import de.bausdorf.simracing.racecontrol.orga.model.RuleViolation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RuleViolationView {
	private long id;

	private String categoryCode;
	private String identifier;
	private String violationReason;
	private Set<String> possiblePenaltyCodes;

	public String getDescription() {
		return identifier
				+ ") "
				+ violationReason;
	}

	public String getPenaltyCodesAsString() {
		StringBuilder penaltyCodes = new StringBuilder();
		possiblePenaltyCodes.stream().forEach(s -> {
			penaltyCodes.append(s);
			penaltyCodes.append(' ');
		});
		return penaltyCodes.toString().trim();
	}

	public static RuleViolationView buildEmpty() {
		return RuleViolationView.builder()
				.categoryCode("")
				.id(0)
				.identifier("")
				.violationReason("")
				.possiblePenaltyCodes(new HashSet<>())
				.build();
	}

	public static RuleViolationView buildFromEntity(@Nullable RuleViolation violation) {
		if(violation == null) {
			return buildEmpty();
		}
		return RuleViolationView.builder()
				.id(violation.getId())
				.categoryCode(violation.getCategory().getCategoryCode())
				.identifier(violation.getIdentifier())
				.violationReason(violation.getViolationReason())
				.possiblePenaltyCodes(violation.getPossiblePenaltyCodes())
				.build();
	}
}
