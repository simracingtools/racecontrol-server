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

import java.util.List;
import java.util.stream.Collectors;

import de.bausdorf.simracing.racecontrol.model.Team;
import de.bausdorf.simracing.racecontrol.util.TimeTools;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class TeamView {
	private TableCellView name;
	private TableCellView carNo;
	private TableCellView avgTeamRating;
	private List<DriverView> drivers;
	private String teamId;

	public static TeamView buildFromTeamList(Team team) {
		return TeamView.builder()
				.name(TableCellView.builder()
						.value(team.getName())
						.displayType(CssClassType.DEFAULT)
						.build())
				.carNo(TableCellView.builder()
						.value(String.valueOf(team.getCarNo()))
						.displayType(CssClassType.DEFAULT)
						.build())
				.teamId(String.valueOf(team.getTeamId()))
				.drivers(team.getDrivers().stream()
						.map(s -> DriverView.builder()
									.name(TableCellView.builder()
											.value(s.getName())
											.displayType(CssClassType.DEFAULT)
											.build())
									.iRacingId(String.valueOf(s.getIracingId()))
									.iRating(String.valueOf(s.getIRating()))
									.stints(s.getStints().stream()
											.map(k -> StintView.builder()
														.startTime(TableCellView.builder()
																.value(TimeTools.shortDurationString(k.getStartTime()))
																.displayType(CssClassType.DEFAULT)
																.build())
														.stopTime(TableCellView.builder()
																.value(TimeTools.shortDurationString(k.getEndTime()))
																.displayType(CssClassType.DEFAULT)
																.build())
														.duration(TableCellView.builder()
																.value(TimeTools.shortDurationString(k.getStintDuration()))
																.displayType(CssClassType.DEFAULT)
																.build())
														.build()
											)
											.collect(Collectors.toList()))
									.build()
						)
						.collect(Collectors.toList()))
				.build();
	}
}
