package de.bausdorf.simracing.racecontrol.impl;

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

import de.bausdorf.simracing.racecontrol.model.Stint;
import de.bausdorf.simracing.racecontrol.util.RacecontrolServerProperties;

@Component
public class RuleComplianceCheck {

	final RacecontrolServerProperties properties;

	private Duration maxStintDuration;
	private Duration maxTimeRequiresRest;
	private Duration minRestDuration;

	public RuleComplianceCheck(@Autowired RacecontrolServerProperties props) {
		this.properties = props;

		this.maxStintDuration = Duration.ofMinutes(this.properties.getMaxDrivingTimeMinutes());
		this.maxTimeRequiresRest = Duration.ofMinutes(this.properties.getMaxDrivingTimeRequiresRestMinutes());
		this.minRestDuration = Duration.ofMinutes(this.properties.getMinRestTimeMinutes());
	}

	public boolean isStintDurationCompliant(Stint stint) {
		return maxStintDuration.compareTo(stint.getStintDuration()) > 0;
	}

	public boolean isRestTimeCompliant(Stint last, Stint next) {
		if(maxTimeRequiresRest.compareTo(last.getStintDuration()) > 0) {
			Duration restTime = next.getStartTime().minus(last.getEndTime());
			return minRestDuration.compareTo(restTime) > 0;
		}
		return true;
	}
}
