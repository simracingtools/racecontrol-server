package de.bausdorf.simracing.racecontrol.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@Builder
@ToString
public class Driver {
	private String name;
	private String id;
	private long iRating;
	private List<Stint> stints;

	public Stint getLastStint() {
		return stints.get(stints.size() - 1);
	}

	public List<Duration> getBreakDurations() {
		List<Duration> breakDurations = new ArrayList<>();
		if( stints.size() > 1) {
			for(int i = 1; i < stints.size(); i++) {
				breakDurations.add(stints.get(i).getStartTime().minus(stints.get(i-1).getEndTime()));
			}
		}
		return breakDurations;
	}
}
