package de.bausdorf.simracing.racecontrol.orga.model;

/*-
 * #%L
 * racecontrol-server
 * %%
 * Copyright (C) 2020 - 2021 bausdorf engineering
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

import de.bausdorf.simracing.racecontrol.orga.api.SkyConditionType;
import de.bausdorf.simracing.racecontrol.orga.api.WindDirectionType;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class TrackSession {
	@Id
	@GeneratedValue
	private long id;

	private long eventId;
	private String title;
	@Convert(converter = OffsetDateTimeConverter.class)
	private OffsetDateTime dateTime;
	private long trackConfigId;
	private Long irSessionId;

	private LocalDateTime simulatedTimeOfDay;
	private long trackUsagePercent;
	private boolean trackStateCarryOver;

	private boolean generatedWeather;
	private Long temperature;
	private Long humidity;
	private Long windSpeed;
	private WindDirectionType windDirection;

	private boolean generatedSky;
	private SkyConditionType sky;
	private Long skyVarInitial;
	private Long skyVarContinued;

	@OneToMany(mappedBy = "trackSessionId")
	@LazyCollection(LazyCollectionOption.FALSE)
	@ToString.Exclude
	private List<TrackSubsession> sessionParts;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
		TrackSession that = (TrackSession) o;
		return id != that.id;
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
