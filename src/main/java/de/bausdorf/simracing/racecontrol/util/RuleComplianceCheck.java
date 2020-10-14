package de.bausdorf.simracing.racecontrol.util;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class RuleComplianceCheck {

	private final Duration maxStintDuration;
	private final Duration maxTimeRequiresRest;
	private final Duration minRestDuration;
	private final double fairShareFactor;
	private final int proAmDiscriminator;

	public RuleComplianceCheck(@Autowired RacecontrolServerProperties props) {
		this.maxStintDuration = Duration.ofMinutes(props.getMaxDrivingTimeMinutes());
		this.maxTimeRequiresRest = Duration.ofMinutes(props.getMaxDrivingTimeRequiresRestMinutes());
		this.minRestDuration = Duration.ofMinutes(props.getMinRestTimeMinutes());
		this.fairShareFactor = props.getFairShareFactor();
		this.proAmDiscriminator = props.getProAmDiscriminator();
	}

	public boolean isStintDurationCompliant(Duration trackTime) {
		return maxStintDuration.compareTo(trackTime) > 0;
	}

	public boolean isRestTimeCompliant(Duration lastDrivingTime, Duration lastStintEnd, Duration nextStintStart) {
		if(maxTimeRequiresRest.compareTo(lastDrivingTime) < 0) {
			Duration restTime = nextStintStart.minus(lastStintEnd);
			return minRestDuration.compareTo(restTime) < 0;
		}
		return true;
	}

	public boolean isFairShareCompliant(int maxDrivingMillis, int drivingMillisToCheck) {
		return (maxDrivingMillis * fairShareFactor) < drivingMillisToCheck;
	}

	public boolean isProTeam(double avgTeamRating) {
		return avgTeamRating > proAmDiscriminator;
	}
}
