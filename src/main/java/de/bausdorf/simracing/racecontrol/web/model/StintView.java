package de.bausdorf.simracing.racecontrol.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class StintView {

	private TableCellView startTime;
	private TableCellView stopTime;
	private TableCellView duration;
}
