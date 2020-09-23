package de.bausdorf.simracing.racecontrol.web.model;

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

	public static TeamView buildFromTeamList(Team team) {
		return TeamView.builder()
				.name(TableCellView.builder()
						.value(team.getName())
						.build())
				.carNo(TableCellView.builder()
						.value(String.valueOf(team.getCarNo()))
						.build())
				.drivers(team.getDrivers().stream()
						.map(s -> DriverView.builder()
									.name(TableCellView.builder()
											.value(s.getName())
											.build())
									.iRacingId(String.valueOf(s.getIracingId()))
									.iRating(String.valueOf(s.getIRating()))
									.stints(s.getStints().stream()
											.map(k -> StintView.builder()
														.startTime(TableCellView.builder()
																.value(TimeTools.shortDurationString(k.getStartTime()))
																.build())
														.stopTime(TableCellView.builder()
																.value(TimeTools.shortDurationString(k.getEndTime()))
																.build())
														.duration(TableCellView.builder()
																.value(TimeTools.shortDurationString(k.getStintDuration()))
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
