package de.bausdorf.simracing.racecontrol.live.model;

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

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import de.bausdorf.simracing.racecontrol.live.api.EventType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Driver extends BaseEntity {
	private String name;
	private long iRating;
	@ManyToOne(cascade = CascadeType.ALL)
	private Team team;
	@OneToMany(mappedBy = "driver", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private List<Stint> stints;
	private EventType lastEventType;
	private double lastLapPosition;

	public Driver() {
		this.stints = new ArrayList<>();
	}

	@Builder
	public Driver(String sessionId, long driverId, String name, long iRating, Team team, EventType lastEventType, double lastLapPosition, List<Stint> stints) {
		super(sessionId, driverId);
		this.name = name;
		this.iRating = iRating;
		this.team = team;
		this.stints = stints;
		this.lastEventType = lastEventType;
		this.lastLapPosition = lastLapPosition;
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

	@Override
	public String toString() {
		return "Driver(name=" + this.getName() + ", iRating=" + this.getIRating() + ", team=" + this.getTeam().getName() + ", stints="
				+ this.getStints() + ")";
	}
}
