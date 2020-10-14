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

import lombok.Getter;

public enum CssClassType {
	DEFAULT("", ""),
	TBL_SUCCESS("table-success", ""),
	TBL_WARNING("table-warning", ""),
	TBL_DANGER("table-danger", ""),
	TBL_INFO("table-info", ""),
	TBL_DARK("table-dark", ""),
	TBL_PRIMARY("table-primary", "");

	@Getter
	String backgroundClass;

	@Getter
	String textClass;

	CssClassType(String backgroundClass, String textClass) {
		this.backgroundClass = backgroundClass;
		this.textClass = textClass;
	}

	public String getClassString() {
		return backgroundClass + " " + textClass;
	}
}
