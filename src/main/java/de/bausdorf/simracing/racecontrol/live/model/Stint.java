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

import java.time.Duration;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import de.bausdorf.simracing.racecontrol.live.api.StintStateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Stint {
	@Id
	@GeneratedValue
	long id;
	private Duration startTime;
	private Duration endTime;
	@ManyToOne(cascade = CascadeType.ALL)
	private Driver driver;
	@Column(nullable = false)
	private String sessionId;
	private StintStateType state;

	public Duration getStintDuration() {
		if( startTime != null && endTime != null ) {
			return endTime.minus(startTime);
		}
		return Duration.ZERO;
	}

	public String toString() {
		return "Stint(id=" + this.getId() + ", startTime=" + this.getStartTime() + ", endTime=" + this.getEndTime() + ", driver="
				+ this.getDriver().getName() + ", sessionId=" + this.getSessionId() + ")";
	}
}
