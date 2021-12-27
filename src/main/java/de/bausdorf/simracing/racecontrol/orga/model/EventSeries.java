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

import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class EventSeries {
	@Id
	@GeneratedValue
	private long id;

	private String title;
	private String logoUrl;
	private String discordLink;
	private String discordInvite;
	@ManyToOne
	RuleSet ruleSet;
	@ManyToOne
	OrganizationalUnit organizingUnit;
	@OneToMany(mappedBy = "id")
	@LazyCollection(LazyCollectionOption.FALSE)
	@ToString.Exclude
	private List<CarClass> carClassPreset;
	@OneToMany(mappedBy = "id")
	@LazyCollection(LazyCollectionOption.FALSE)
	@ToString.Exclude
	private List<TrackEvent> trackEvents;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
		EventSeries that = (EventSeries) o;
		return id != that.id;
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
