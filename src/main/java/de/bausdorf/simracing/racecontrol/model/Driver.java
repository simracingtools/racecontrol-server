package de.bausdorf.simracing.racecontrol.model;

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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
public class Driver extends BaseEntity {
	private String name;
	private long iRating;
	@ManyToOne(cascade = CascadeType.ALL)
	private Team team;
	@OneToMany(mappedBy = "driver", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private List<Stint> stints;

	public Driver() {
		this.stints = new ArrayList<>();
	}

	@Builder
	public Driver(String sessionId, long driverId, String name, long iRating, Team team, List<Stint> stints) {
		super(sessionId, driverId);
		this.name = name;
		this.iRating = iRating;
		this.team = team;
		this.stints = stints;
	}

	public long getDriverId() {
		return super.getIracingId();
	}

	public Stint getLastStint() {
		if(stints.isEmpty()) {
			return null;
		}
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
