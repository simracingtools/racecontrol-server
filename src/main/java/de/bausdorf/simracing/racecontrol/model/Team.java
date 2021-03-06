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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Team extends BaseEntity {
	private String name;
	private long currentDriverId;
	String carNo;
	String carName;
	String carClass;
	long carClassId;
	String carClassColor;
	@OneToMany(mappedBy = "team", fetch = FetchType.EAGER)
	private List<Driver> drivers;

	public Team() {
		drivers = new ArrayList<>();
	}

	@Builder
	public Team(String sessionId, long teamId, String name, long currentDriverId, String carNo,
			String carName, String carClass, long carClassId, String carClassColor, List<Driver> drivers) {
		super(sessionId, teamId);
		this.name = name;
		this.currentDriverId = currentDriverId;
		this.carNo = carNo;
		this.carName = carName;
		this.carClass = carClass;
		this.carClassId = carClassId;
		this.drivers = drivers;
		this.carClassColor = carClassColor;
	}

	public long getTeamId() {
		return super.getIracingId();
	}

	public boolean containsDriver(long driverId) {
		return drivers.stream()
				.filter(s -> s.getIracingId() == driverId)
				.collect(Collectors.toSet()).stream().findFirst().isPresent();
	}

	@Override
	public String toString() {
		return "Team(name=" + this.getName() + ", currentDriverId=" + this.getCurrentDriverId() + ", carNo=" + this.getCarNo() + ", drivers="
				+ this.getDrivers() + ")";
	}
}
