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

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class SessionView {

	private String sessionId;
	private TableCellView trackName;
	private TableCellView sessionDuration;
	private TableCellView sessionType;
	private TableCellView sessionState;
	private List<TeamView> teams;
	private int maxStintColumns;

	public List<Integer> getMaxDriverStints() {
		int maxStints = 0;
		for (TeamView team : teams) {
			if (maxStints < team.getMaxDriverStints()) {
				maxStints = team.getMaxDriverStints();
			}
		}
		ArrayList<Integer> retVal = new ArrayList<>();
		for (int i = 0; i < maxStints; i++) {
			retVal.add(i + 1);
		}
		return retVal;
	}
}
