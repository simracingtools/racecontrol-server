package de.bausdorf.simracing.racecontrol.impl;

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
