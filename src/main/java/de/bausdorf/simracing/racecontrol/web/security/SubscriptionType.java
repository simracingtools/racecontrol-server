package de.bausdorf.simracing.racecontrol.web.security;

import java.time.Duration;

import lombok.Getter;

@Getter
public enum SubscriptionType {
	NONE("None", Duration.ZERO),
	WEEKLY("Weekly", Duration.ofDays(7)),
	ONE_MONTH("Monthly", Duration.ofDays(30)),
	ONE_YEAR( "Yearly", Duration.ofDays(365)),
	LIFETIME("Lifetime", Duration.ofDays(18250));

	private final String description;
	private final Duration duration;

	SubscriptionType(String name, Duration duration) {
		this.description = name;
		this.duration = duration;
	}
}
