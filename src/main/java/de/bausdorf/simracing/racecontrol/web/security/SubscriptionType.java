package de.bausdorf.simracing.racecontrol.web.security;

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
