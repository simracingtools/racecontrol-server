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

import org.springframework.lang.Nullable;

import de.bausdorf.simracing.racecontrol.orga.model.RuleViolationCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ViolationCategoryView {

	private String code;
	private String description;

	public void updateEntity(RuleViolationCategory category) {
		category.setCategoryCode(code);
		category.setCategoryName(description);
	}

	public static ViolationCategoryView buildFromEntity(@Nullable RuleViolationCategory category) {
		if(category == null) {
			return buildEmpty();
		}
		return ViolationCategoryView.builder()
				.code(category.getCategoryCode())
				.description(category.getCategoryName())
				.build();
	}

	public static ViolationCategoryView buildEmpty() {
		return ViolationCategoryView.builder()
				.code("")
				.description("")
				.build();
	}
}
