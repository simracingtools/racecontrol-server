package de.bausdorf.simracing.racecontrol.web.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class DriverView {
	private String iRacingId;
	private TableCellView name;
	private String iRating;
	private List<StintView> stints;
	private TableCellView drivingTime;
}
