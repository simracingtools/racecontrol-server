package de.bausdorf.simracing.racecontrol.web.model;

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
	private List<TeamView> teams;
	private int maxStintColumns;
}
